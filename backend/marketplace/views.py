from decimal import Decimal
from django.db import transaction
from django.db.models import Count, Sum
from django.db.models.functions import TruncDate
from rest_framework import permissions, status
from rest_framework.authtoken.models import Token
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response

from .models import CommissionLog, CustomUser, FarmerProfile, Order, PilotProfile
from .serializers import (
    CommissionLogSerializer,
    FarmerProfileSerializer,
    OrderSerializer,
    PilotProfileSerializer,
)
from .services import (
    accept_order_atomic,
    calculate_polygon_area_acres,
    calculate_tariff_split,
    complete_order_atomic,
    compute_pilot_priority_score,
    create_razorpay_order,
)


class IsKrishivantriAdmin(permissions.BasePermission):
    def has_permission(self, request, view):
        return bool(
            request.user
            and request.user.is_authenticated
            and (request.user.role == "admin" or request.user.is_staff)
        )


@api_view(["POST"])
@permission_classes([permissions.AllowAny])
def register_pilot(request):
    data = request.data
    phone = data.get("phone", "").strip()
    if CustomUser.objects.filter(phone=phone).exists():
        return Response({"detail": "Phone already registered."}, status=status.HTTP_400_BAD_REQUEST)

    with transaction.atomic():
        user = CustomUser.objects.create_user(
            phone=phone,
            name=data.get("name", ""),
            role="pilot",
        )
        PilotProfile.objects.create(
            user=user,
            drone_type=data.get("drone_type", "quadcopter"),
            tank_capacity=Decimal(str(data.get("tank_capacity", "10"))),
            license_document=request.FILES.get("license_document"),
            license_document_url=data.get("license_document_url", ""),
            bank_upi_id=data.get("bank_upi_id", ""),
        )
        token, _ = Token.objects.get_or_create(user=user)
    return Response(
        {"user_id": str(user.id), "name": user.name, "phone": user.phone, "role": user.role, "token": token.key},
        status=status.HTTP_201_CREATED,
    )


@api_view(["POST"])
@permission_classes([permissions.AllowAny])
def register_farmer(request):
    data = request.data
    phone = data.get("phone", "").strip()
    if CustomUser.objects.filter(phone=phone).exists():
        return Response({"detail": "Phone already registered."}, status=status.HTTP_400_BAD_REQUEST)

    with transaction.atomic():
        user = CustomUser.objects.create_user(
            phone=phone,
            name=data.get("name", ""),
            role="farmer",
        )
        FarmerProfile.objects.create(
            user=user,
            farmer_id_number=data.get("farmer_id_number", "").strip().upper(),
        )
        token, _ = Token.objects.get_or_create(user=user)
    return Response(
        {"user_id": str(user.id), "name": user.name, "phone": user.phone, "role": user.role, "token": token.key},
        status=status.HTTP_201_CREATED,
    )


@api_view(["POST"])
@permission_classes([permissions.AllowAny])
def login_view(request):
    phone = request.data.get("phone", "").strip()
    role = request.data.get("role", "").strip()
    user = CustomUser.objects.filter(phone=phone, role=role).first()
    if not user:
        return Response({"detail": "Invalid credentials."}, status=status.HTTP_401_UNAUTHORIZED)
    if role == "admin":
        password = request.data.get("password", "")
        if not user.check_password(password):
            return Response({"detail": "Invalid admin credentials."}, status=status.HTTP_401_UNAUTHORIZED)
    token, _ = Token.objects.get_or_create(user=user)
    return Response(
        {"user_id": str(user.id), "name": user.name, "phone": user.phone, "role": user.role, "token": token.key}
    )


@api_view(["GET", "POST"])
def farmer_orders(request):
    if request.user.role != "farmer":
        return Response({"detail": "Farmer access required."}, status=status.HTTP_403_FORBIDDEN)
    farmer = request.user.farmer_profile

    if request.method == "GET":
        qs = Order.objects.filter(farmer=farmer)
        return Response(OrderSerializer(qs, many=True).data)

    coords = request.data.get("field_polygon_coordinates", [])
    acres = calculate_polygon_area_acres(coords)
    if acres <= Decimal("0.00"):
        return Response({"detail": "Draw a valid closed farmland polygon (>= 3 points)."}, status=400)

    total, admin_comm, pilot_earn = calculate_tariff_split(acres)
    payment_method = request.data.get("payment_method", "online")

    rzp_order_id = ""
    payment_status = "pending"
    if payment_method == "online":
        rzp = create_razorpay_order(total, f"kv_{farmer.id}")
        rzp_order_id = rzp.get("id", "")
        if request.data.get("razorpay_payment_id"):
            payment_status = "paid"

    order = Order.objects.create(
        farmer=farmer,
        field_polygon_coordinates=coords,
        acres_to_spray=acres,
        location_address=request.data.get("location_address", ""),
        total_amount=total,
        admin_commission=admin_comm,
        pilot_earnings=pilot_earn,
        payment_method=payment_method,
        payment_status=payment_status,
        razorpay_order_id=rzp_order_id,
        razorpay_payment_id=request.data.get("razorpay_payment_id", ""),
    )
    return Response(OrderSerializer(order).data, status=status.HTTP_201_CREATED)


@api_view(["POST"])
def rate_pilot(request, order_id):
    if request.user.role != "farmer":
        return Response({"detail": "Farmer access required."}, status=status.HTTP_403_FORBIDDEN)
    stars = max(1, min(5, int(request.data.get("rating", 5))))
    with transaction.atomic():
        order = Order.objects.select_for_update().get(pk=order_id, farmer=request.user.farmer_profile)
        if order.order_status != "completed" or order.assigned_pilot is None:
            return Response({"detail": "Only completed orders with an assigned pilot can be rated."}, status=400)
        if order.pilot_rating is not None:
            return Response({"detail": "Order already rated."}, status=400)
        order.pilot_rating = stars
        order.save(update_fields=["pilot_rating"])

        pilot = PilotProfile.objects.select_for_update().get(pk=order.assigned_pilot_id)
        total_score = (pilot.rating_average * pilot.rating_count) + Decimal(stars)
        pilot.rating_count += 1
        pilot.rating_average = (total_score / pilot.rating_count).quantize(Decimal("0.01"))
        pilot.save(update_fields=["rating_count", "rating_average"])
    return Response({"success": True, "rating_average": str(pilot.rating_average)})


@api_view(["POST"])
def toggle_pilot_online(request):
    if request.user.role != "pilot":
        return Response({"detail": "Pilot access required."}, status=status.HTTP_403_FORBIDDEN)
    pilot = request.user.pilot_profile
    pilot.is_online = bool(request.data.get("is_online", not pilot.is_online))
    pilot.save(update_fields=["is_online"])
    return Response({"success": True, "is_online": pilot.is_online})


@api_view(["GET"])
def pilot_queue(request):
    if request.user.role != "pilot":
        return Response({"detail": "Pilot access required."}, status=status.HTTP_403_FORBIDDEN)
    pilot = request.user.pilot_profile
    if not pilot.is_online:
        return Response([])

    priority = compute_pilot_priority_score(pilot)
    pending = Order.objects.filter(order_status="pending")
    if priority >= 115.0:
        pending = pending.order_by("-acres_to_spray", "-created_at")
    else:
        pending = pending.order_by("-created_at")
    return Response(OrderSerializer(pending, many=True).data)


@api_view(["POST"])
def accept_order(request, order_id):
    if request.user.role != "pilot":
        return Response({"detail": "Pilot access required."}, status=status.HTTP_403_FORBIDDEN)
    ok, msg = accept_order_atomic(request.user.pilot_profile.pk, order_id)
    if not ok:
        return Response({"detail": msg}, status=status.HTTP_400_BAD_REQUEST)
    order = Order.objects.get(pk=order_id)
    return Response(OrderSerializer(order).data)


@api_view(["POST"])
def complete_order(request, order_id):
    if request.user.role != "pilot":
        return Response({"detail": "Pilot access required."}, status=status.HTTP_403_FORBIDDEN)
    order = complete_order_atomic(order_id)
    return Response(OrderSerializer(order).data)


@api_view(["POST"])
def wallet_topup(request):
    if request.user.role != "pilot":
        return Response({"detail": "Pilot access required."}, status=status.HTTP_403_FORBIDDEN)
    amount = Decimal(str(request.data.get("amount", "0")))
    if amount <= Decimal("0.00"):
        return Response({"detail": "Positive top-up amount required."}, status=400)
    with transaction.atomic():
        pilot = PilotProfile.objects.select_for_update().get(pk=request.user.pilot_profile.pk)
        pilot.wallet_balance = (pilot.wallet_balance + amount).quantize(Decimal("0.01"))
        pilot.save(update_fields=["wallet_balance"])
    return Response({"success": True, "wallet_balance": str(pilot.wallet_balance)})


@api_view(["GET"])
@permission_classes([IsKrishivantriAdmin])
def admin_overview(request):
    pilots = PilotProfile.objects.select_related("user").all()
    farmers = FarmerProfile.objects.select_related("user").all()
    daily_volume = list(
        Order.objects.annotate(day=TruncDate("created_at"))
        .values("day")
        .annotate(order_count=Count("id"), revenue=Sum("total_amount"))
        .order_by("-day")[:14]
    )
    pilot_performance = list(
        PilotProfile.objects.select_related("user")
        .annotate(
            completed_orders=Count("orders", filter=models_Q_completed()),
            total_earnings=Sum("orders__pilot_earnings", filter=models_Q_completed()),
        )
        .values("id", "user__name", "drone_type", "rating_average", "is_license_verified", "completed_orders", "total_earnings")
    )
    logs = CommissionLog.objects.all()[:50]
    return Response(
        {
            "total_pilots": pilots.count(),
            "verified_pilots": pilots.filter(is_license_verified=True).count(),
            "total_farmers": farmers.count(),
            "total_orders": Order.objects.count(),
            "daily_order_volume": daily_volume,
            "pilot_performance": pilot_performance,
            "pilots": PilotProfileSerializer(pilots, many=True).data,
            "farmers": FarmerProfileSerializer(farmers, many=True).data,
            "commission_logs": CommissionLogSerializer(logs, many=True).data,
        }
    )


def models_Q_completed():
    from django.db.models import Q
    return Q(orders__order_status="completed")


@api_view(["POST"])
@permission_classes([IsKrishivantriAdmin])
def verify_pilot_license(request, pilot_id):
    pilot = PilotProfile.objects.get(pk=pilot_id)
    pilot.is_license_verified = bool(request.data.get("verified", True))
    pilot.save(update_fields=["is_license_verified"])
    return Response({"success": True, "is_license_verified": pilot.is_license_verified})
