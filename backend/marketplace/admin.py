from django.contrib import admin
from .models import (
    CommissionLog,
    CustomUser,
    EmailOtpChallenge,
    FarmerProfile,
    Order,
    PilotProfile,
    SmtpConfiguration,
)


@admin.register(CustomUser)
class CustomUserAdmin(admin.ModelAdmin):
    list_display = ("name", "email", "phone", "role", "created_at")
    list_filter = ("role",)
    search_fields = ("name", "email", "phone")


@admin.register(PilotProfile)
class PilotProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "drone_type", "tank_capacity", "is_license_verified", "wallet_balance", "is_online", "rating_average")
    list_filter = ("drone_type", "is_license_verified", "is_online")


@admin.register(FarmerProfile)
class FarmerProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "farmer_id_number", "coins")


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("id", "farmer", "assigned_pilot", "acres_to_spray", "total_amount", "payment_method", "order_status", "created_at")
    list_filter = ("order_status", "payment_method", "payment_status")


@admin.register(CommissionLog)
class CommissionLogAdmin(admin.ModelAdmin):
    list_display = ("order", "pilot", "amount", "payment_method", "created_at")


@admin.register(SmtpConfiguration)
class SmtpConfigurationAdmin(admin.ModelAdmin):
    list_display = ("host_email", "smtp_host", "smtp_port", "use_tls", "use_ssl", "is_active", "updated_at")
    list_filter = ("is_active", "use_tls", "use_ssl")


@admin.register(EmailOtpChallenge)
class EmailOtpChallengeAdmin(admin.ModelAdmin):
    list_display = ("email", "attempts_count", "is_used", "expires_at", "created_at")
    list_filter = ("is_used",)
    readonly_fields = ("otp_hash", "created_at")
