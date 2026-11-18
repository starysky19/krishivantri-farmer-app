from django.urls import path
from . import views

urlpatterns = [
    path("auth/pilot/register/", views.register_pilot, name="register_pilot"),
    path("auth/farmer/register/", views.register_farmer, name="register_farmer"),
    path("auth/login/", views.login_view, name="login"),
    path("farmer/orders/", views.farmer_orders, name="farmer_orders"),
    path("farmer/orders/<uuid:order_id>/rate/", views.rate_pilot, name="rate_pilot"),
    path("pilot/toggle-online/", views.toggle_pilot_online, name="toggle_pilot_online"),
    path("pilot/queue/", views.pilot_queue, name="pilot_queue"),
    path("pilot/orders/<uuid:order_id>/accept/", views.accept_order, name="accept_order"),
    path("pilot/orders/<uuid:order_id>/complete/", views.complete_order, name="complete_order"),
    path("pilot/wallet/topup/", views.wallet_topup, name="wallet_topup"),
    path("admin/overview/", views.admin_overview, name="admin_overview"),
    path("admin/pilots/<int:pilot_id>/verify/", views.verify_pilot_license, name="verify_pilot_license"),
]
