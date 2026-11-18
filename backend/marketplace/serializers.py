from rest_framework import serializers
from .models import CommissionLog, CustomUser, FarmerProfile, Order, PilotProfile


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = CustomUser
        fields = ["id", "phone", "name", "role", "created_at"]


class PilotProfileSerializer(serializers.ModelSerializer):
    user = UserSerializer(read_only=True)

    class Meta:
        model = PilotProfile
        fields = [
            "id",
            "user",
            "drone_type",
            "tank_capacity",
            "license_document",
            "license_document_url",
            "is_license_verified",
            "bank_upi_id",
            "wallet_balance",
            "is_online",
            "rating_average",
            "rating_count",
        ]


class FarmerProfileSerializer(serializers.ModelSerializer):
    user = UserSerializer(read_only=True)

    class Meta:
        model = FarmerProfile
        fields = ["id", "user", "farmer_id_number", "coins"]


class OrderSerializer(serializers.ModelSerializer):
    farmer_name = serializers.CharField(source="farmer.user.name", read_only=True)
    pilot_name = serializers.CharField(source="assigned_pilot.user.name", read_only=True, allow_null=True)

    class Meta:
        model = Order
        fields = [
            "id",
            "farmer_name",
            "pilot_name",
            "field_polygon_coordinates",
            "acres_to_spray",
            "location_address",
            "total_amount",
            "admin_commission",
            "pilot_earnings",
            "payment_method",
            "payment_status",
            "order_status",
            "pilot_eta_minutes",
            "pilot_rating",
            "created_at",
        ]


class CommissionLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = CommissionLog
        fields = ["id", "order", "pilot", "amount", "payment_method", "note", "created_at"]
