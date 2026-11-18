import uuid
from decimal import Decimal
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models
from django.utils import timezone


class CustomUserManager(BaseUserManager):
    def create_user(self, phone, name, role="farmer", password=None, **extra_fields):
        if not phone:
            raise ValueError("Phone number is required.")
        user = self.model(phone=phone.strip(), name=name.strip(), role=role, **extra_fields)
        if password:
            user.set_password(password)
        else:
            user.set_unusable_password()
        user.save(using=self._db)
        return user

    def create_superuser(self, phone, name="Admin", password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        return self.create_user(phone=phone, name=name, role="admin", password=password, **extra_fields)


class CustomUser(AbstractBaseUser, PermissionsMixin):
    ROLE_CHOICES = (
        ("admin", "Admin"),
        ("pilot", "Drone Pilot"),
        ("farmer", "Farmer"),
    )

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone = models.CharField(max_length=20, unique=True)
    name = models.CharField(max_length=150)
    role = models.CharField(max_length=16, choices=ROLE_CHOICES, default="farmer")
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    created_at = models.DateTimeField(default=timezone.now)

    USERNAME_FIELD = "phone"
    REQUIRED_FIELDS = ["name"]

    objects = CustomUserManager()

    def __str__(self):
        return f"{self.name} ({self.role})"


class PilotProfile(models.Model):
    DRONE_TYPE_CHOICES = (
        ("quadcopter", "Quadcopter"),
        ("hexacopter", "Hexacopter"),
    )

    user = models.OneToOneField(CustomUser, on_delete=models.CASCADE, related_name="pilot_profile")
    drone_type = models.CharField(max_length=20, choices=DRONE_TYPE_CHOICES)
    tank_capacity = models.DecimalField(max_digits=6, decimal_places=2)
    license_document = models.FileField(upload_to="pilot_licenses/", blank=True, null=True)
    license_document_url = models.URLField(blank=True, default="")
    is_license_verified = models.BooleanField(default=False)
    bank_upi_id = models.CharField(max_length=120)
    wallet_balance = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal("0.00"))
    is_online = models.BooleanField(default=False)
    rating_average = models.DecimalField(max_digits=3, decimal_places=2, default=Decimal("5.00"))
    rating_count = models.PositiveIntegerField(default=0)

    def __str__(self):
        return f"Pilot: {self.user.name} ({self.drone_type})"


class FarmerProfile(models.Model):
    user = models.OneToOneField(CustomUser, on_delete=models.CASCADE, related_name="farmer_profile")
    farmer_id_number = models.CharField(max_length=64, unique=True)
    # Gamified virtual reward points only; not redeemable for real cash
    coins = models.PositiveIntegerField(default=0)

    def __str__(self):
        return f"Farmer: {self.user.name} ({self.farmer_id_number})"


class Order(models.Model):
    PAYMENT_METHOD_CHOICES = (
        ("online", "Razorpay UPI (Online)"),
        ("cash", "Cash on Field"),
    )
    PAYMENT_STATUS_CHOICES = (
        ("pending", "Pending"),
        ("paid", "Paid"),
    )
    ORDER_STATUS_CHOICES = (
        ("pending", "Pending"),
        ("accepted", "Accepted"),
        ("in_progress", "In Progress"),
        ("completed", "Completed"),
        ("cancelled", "Cancelled"),
    )

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    farmer = models.ForeignKey(FarmerProfile, on_delete=models.CASCADE, related_name="orders")
    assigned_pilot = models.ForeignKey(
        PilotProfile, on_delete=models.SET_NULL, null=True, blank=True, related_name="orders"
    )
    field_polygon_coordinates = models.JSONField(default=list)
    acres_to_spray = models.DecimalField(max_digits=8, decimal_places=2)
    location_address = models.TextField()
    total_amount = models.DecimalField(max_digits=10, decimal_places=2)
    admin_commission = models.DecimalField(max_digits=10, decimal_places=2)
    pilot_earnings = models.DecimalField(max_digits=10, decimal_places=2)
    payment_method = models.CharField(max_length=16, choices=PAYMENT_METHOD_CHOICES)
    payment_status = models.CharField(max_length=16, choices=PAYMENT_STATUS_CHOICES, default="pending")
    order_status = models.CharField(max_length=20, choices=ORDER_STATUS_CHOICES, default="pending")
    pilot_eta_minutes = models.PositiveIntegerField(default=15)
    pilot_rating = models.PositiveSmallIntegerField(null=True, blank=True)
    razorpay_order_id = models.CharField(max_length=100, blank=True, default="")
    razorpay_payment_id = models.CharField(max_length=100, blank=True, default="")
    created_at = models.DateTimeField(default=timezone.now)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"Order {self.id} ({self.acres_to_spray} acres)"


class CommissionLog(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name="commission_logs")
    pilot = models.ForeignKey(PilotProfile, on_delete=models.CASCADE, related_name="commission_logs")
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    payment_method = models.CharField(max_length=16)
    note = models.CharField(max_length=255)
    created_at = models.DateTimeField(default=timezone.now)

    class Meta:
        ordering = ["-created_at"]
