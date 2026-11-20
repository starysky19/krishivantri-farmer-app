from datetime import timedelta
from email.message import EmailMessage
import math
import secrets
import smtplib
from decimal import Decimal, ROUND_HALF_UP
import razorpay
from django.conf import settings
from django.contrib.auth.hashers import check_password, make_password
from django.db import transaction
from django.db.models import F
from django.utils import timezone

from .models import (
    CommissionLog,
    CustomUser,
    EmailOtpChallenge,
    Order,
    PilotProfile,
    SmtpConfiguration,
)

TARIFF_PER_ACRE = Decimal("450.00")
COMMISSION_RATE = Decimal("0.10")
EARTH_RADIUS_METERS = 6371000.0
SQ_METERS_PER_ACRE = 4046.8564224


def calculate_polygon_area_acres(coordinates: list[dict]) -> Decimal:
    """
    Computes geodesic polygon area in acres from GPS coordinates [{'lat': ..., 'lng': ...}].
    """
    if not coordinates or len(coordinates) < 3:
        return Decimal("0.00")

    avg_lat_rad = sum(math.radians(float(pt["lat"])) for pt in coordinates) / len(coordinates)
    cos_lat = math.cos(avg_lat_rad)
    xy = [
        (
            EARTH_RADIUS_METERS * math.radians(float(pt["lng"])) * cos_lat,
            EARTH_RADIUS_METERS * math.radians(float(pt["lat"])),
        )
        for pt in coordinates
    ]
    area_sq_m = abs(
        sum(xy[i][0] * xy[(i + 1) % len(xy)][1] - xy[(i + 1) % len(xy)][0] * xy[i][1] for i in range(len(xy)))
    ) / 2.0
    acres = Decimal(str(area_sq_m / SQ_METERS_PER_ACRE)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return acres


def calculate_tariff_split(acres: Decimal) -> tuple[Decimal, Decimal, Decimal]:
    total = (acres * TARIFF_PER_ACRE).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    admin_commission = (total * COMMISSION_RATE).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    pilot_earnings = (total - admin_commission).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return total, admin_commission, pilot_earnings


def compute_pilot_priority_score(pilot: PilotProfile) -> float:
    drone_boost = 25.0 if pilot.drone_type == "hexacopter" else 10.0
    verified_boost = 15.0 if pilot.is_license_verified else 0.0
    return float(pilot.rating_average) * 20.0 + drone_boost + float(pilot.tank_capacity) * 0.5 + verified_boost


def create_razorpay_order(amount_inr: Decimal, receipt_id: str) -> dict:
    if not settings.RAZORPAY_KEY_ID or not settings.RAZORPAY_KEY_SECRET:
        return {"id": f"order_local_{receipt_id}", "amount": int(amount_inr * 100), "currency": "INR"}
    client = razorpay.Client(auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET))
    return client.order.create(
        {
            "amount": int(amount_inr * 100),
            "currency": "INR",
            "receipt": receipt_id,
            "payment_capture": 1,
        }
    )


def accept_order_atomic(pilot_id, order_id) -> tuple[bool, str]:
    """
    Atomically assigns an order to a pilot.
    Enforces the Cash Order Wallet Rule: wallet_balance >= 10% admin_commission.
    Deducts that 10% commission immediately upon acceptance.
    """
    with transaction.atomic():
        pilot = PilotProfile.objects.select_for_update().get(pk=pilot_id)
        order = Order.objects.select_for_update().get(pk=order_id)

        if order.order_status != "pending":
            return False, "Order is no longer pending."

        if order.payment_method == "cash":
            if pilot.wallet_balance < order.admin_commission:
                return (
                    False,
                    f"Insufficient wallet balance. Need ₹{order.admin_commission} (10% commission) to accept Cash on Field orders.",
                )
            pilot.wallet_balance = (pilot.wallet_balance - order.admin_commission).quantize(Decimal("0.01"))
            pilot.save(update_fields=["wallet_balance"])
            CommissionLog.objects.create(
                order=order,
                pilot=pilot,
                amount=order.admin_commission,
                payment_method="cash",
                note="10% Cash Order Commission deducted on acceptance",
            )

        order.assigned_pilot = pilot
        order.order_status = "accepted"
        order.pilot_eta_minutes = 12 if pilot.drone_type == "hexacopter" else 16
        order.save(update_fields=["assigned_pilot", "order_status", "pilot_eta_minutes"])
        return True, "Order accepted."


def pilot_finish_spraying_atomic(order_id) -> Order:
    """
    Pilot finishes spraying mission.
    Status transitions to 'awaiting_confirmation'.
    Payout remains held in escrow until farmer clicks 'Job Done'.
    """
    with transaction.atomic():
        order = Order.objects.select_for_update().get(pk=order_id)
        if order.order_status not in ["completed", "awaiting_confirmation"]:
            order.order_status = "awaiting_confirmation"
            order.pilot_eta_minutes = 0
            order.save(update_fields=["order_status", "pilot_eta_minutes"])
        return order


def confirm_job_done_atomic(order_id, farmer_id=None) -> tuple[bool, str, Order | None]:
    """
    Farmer clicks 'Job Done':
    - Transitions order to 'completed' & 'paid'.
    - Transfers pilot earnings directly to pilot's wallet balance for online payments.
    - Awards +1 Harvest Coin to the farmer.
    - Settles platform commission.
    """
    with transaction.atomic():
        qs = Order.objects.select_for_update().select_related("farmer", "assigned_pilot")
        if farmer_id:
            qs = qs.filter(farmer_id=farmer_id)
        order = qs.filter(pk=order_id).first()
        if not order:
            return False, "Order not found.", None

        if order.order_status == "completed":
            return False, "Job already confirmed and completed.", order

        if order.order_status not in ["awaiting_confirmation", "in_progress"]:
            return False, "Pilot must complete spraying before you can click Job Done.", order

        order.order_status = "completed"
        order.payment_status = "paid"
        order.pilot_eta_minutes = 0
        order.save(update_fields=["order_status", "payment_status", "pilot_eta_minutes"])

        # Credit pilot's wallet upon farmer Job Done
        if order.payment_method == "online" and order.assigned_pilot is not None:
            pilot = PilotProfile.objects.select_for_update().get(pk=order.assigned_pilot.pk)
            pilot.wallet_balance = (pilot.wallet_balance + order.pilot_earnings).quantize(Decimal("0.01"))
            pilot.save(update_fields=["wallet_balance"])

            CommissionLog.objects.create(
                order=order,
                pilot=order.assigned_pilot,
                amount=order.admin_commission,
                payment_method="online",
                note=f"10% Online Commission settled & ₹{order.pilot_earnings} released to pilot wallet on Farmer Job Done",
            )

        # Gamification: 1 Order = 1 Harvest Coin
        order.farmer.coins = F("coins") + 1
        order.farmer.save(update_fields=["coins"])
        order.refresh_from_db()
        return True, "Job done confirmed! Payout transferred to pilot wallet.", order


def complete_order_atomic(order_id) -> Order:
    return pilot_finish_spraying_atomic(order_id)


OTP_EXPIRY_MINUTES = 5
OTP_RESEND_COOLDOWN_SECONDS = 60
OTP_MAX_REQUESTS_PER_WINDOW = 5
OTP_REQUEST_WINDOW_MINUTES = 15
OTP_MAX_VERIFY_ATTEMPTS = 5


def _send_otp_via_db_smtp(recipient_email: str, otp_code: str) -> None:
    smtp_cfg = SmtpConfiguration.objects.filter(is_active=True).order_by("-updated_at").first()
    if not smtp_cfg:
        raise RuntimeError("Active SMTP configuration is not set in the database.")

    msg = EmailMessage()
    msg["Subject"] = "Your NexusTekFusion Krishivantri 6-Digit Login OTP"
    msg["From"] = f"{smtp_cfg.sender_name} <{smtp_cfg.host_email}>"
    msg["To"] = recipient_email
    msg.set_content(
        f"Your 6-digit login verification code is: {otp_code}\n\n"
        f"Enter this 6-digit OTP in the Krishivantri app. It expires in {OTP_EXPIRY_MINUTES} minutes.\n"
        "Do not share this code with anyone.\n\n"
        "— NexusTekFusion Team"
    )

    if smtp_cfg.use_ssl:
        with smtplib.SMTP_SSL(smtp_cfg.smtp_host, smtp_cfg.smtp_port, timeout=15) as server:
            server.login(smtp_cfg.host_email, smtp_cfg.smtp_password)
            server.send_message(msg)
    else:
        with smtplib.SMTP(smtp_cfg.smtp_host, smtp_cfg.smtp_port, timeout=15) as server:
            if smtp_cfg.use_tls:
                server.starttls()
            server.login(smtp_cfg.host_email, smtp_cfg.smtp_password)
            server.send_message(msg)


def request_email_otp_service(email: str) -> tuple[bool, int, str]:
    """
    Generates a hashed 6-digit OTP, stores it with 5-minute expiry and 60s cooldown,
    and sends it using the active DB SmtpConfiguration.
    Returns (ok, status_code, message) without revealing whether email is registered.
    """
    clean_email = email.strip().lower()
    now = timezone.now()
    window_start = now - timedelta(minutes=OTP_REQUEST_WINDOW_MINUTES)

    recent_count = EmailOtpChallenge.objects.filter(
        email=clean_email,
        created_at__gte=window_start,
    ).count()
    if recent_count >= OTP_MAX_REQUESTS_PER_WINDOW:
        return False, 429, "Too many OTP requests. Please try again later."

    latest = EmailOtpChallenge.objects.filter(email=clean_email, is_used=False).first()
    if latest and latest.resend_available_at > now:
        wait_sec = max(1, int((latest.resend_available_at - now).total_seconds()))
        return False, 429, f"Please wait {wait_sec} seconds before requesting a new OTP."

    user_exists = CustomUser.objects.filter(email__iexact=clean_email, is_active=True).exists()
    otp_code = str(secrets.randbelow(900000) + 100000)

    with transaction.atomic():
        EmailOtpChallenge.objects.filter(email=clean_email, is_used=False).update(is_used=True)
        challenge = EmailOtpChallenge.objects.create(
            email=clean_email,
            otp_hash=make_password(otp_code),
            expires_at=now + timedelta(minutes=OTP_EXPIRY_MINUTES),
            resend_available_at=now + timedelta(seconds=OTP_RESEND_COOLDOWN_SECONDS),
        )

    if user_exists:
        try:
            _send_otp_via_db_smtp(clean_email, otp_code)
        except Exception:
            challenge.is_used = True
            challenge.save(update_fields=["is_used"])
            return False, 503, "Unable to deliver OTP email right now. Please try again shortly."

    return True, 200, "If an account exists for this email, a 6-digit OTP has been sent. It expires in 5 minutes."


def verify_email_otp_service(email: str, otp: str) -> tuple[bool, CustomUser | None, str]:
    clean_email = email.strip().lower()
    clean_otp = otp.strip()
    if len(clean_otp) != 6 or not clean_otp.isdigit():
        return False, None, "Enter a valid 6-digit OTP."
    now = timezone.now()

    with transaction.atomic():
        challenge = (
            EmailOtpChallenge.objects.select_for_update()
            .filter(email=clean_email, is_used=False)
            .order_by("-created_at")
            .first()
        )
        if not challenge or challenge.expires_at < now:
            return False, None, "Invalid or expired OTP."

        if challenge.attempts_count >= OTP_MAX_VERIFY_ATTEMPTS:
            challenge.is_used = True
            challenge.save(update_fields=["is_used"])
            return False, None, "Too many failed verification attempts. Please request a new OTP."

        if not check_password(clean_otp, challenge.otp_hash):
            challenge.attempts_count += 1
            if challenge.attempts_count >= OTP_MAX_VERIFY_ATTEMPTS:
                challenge.is_used = True
                challenge.save(update_fields=["attempts_count", "is_used"])
            else:
                challenge.save(update_fields=["attempts_count"])
            return False, None, "Invalid or expired OTP."

        challenge.is_used = True
        challenge.save(update_fields=["is_used"])

    user = CustomUser.objects.filter(email__iexact=clean_email, is_active=True).first()
    if not user:
        return False, None, "Invalid or expired OTP."
    return True, user, "Verified."
