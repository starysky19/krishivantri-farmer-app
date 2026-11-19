import math
from decimal import Decimal, ROUND_HALF_UP
import razorpay
from django.conf import settings
from django.db import transaction
from django.db.models import F

from .models import CommissionLog, Order, PilotProfile

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
