# Do not change manufacturer/model: Samsung hardware and vendor blobs depend on them.
PRODUCT_PRODUCT_PROPERTIES += \
    ro.xos.name=XonolithOS \
    ro.xos.short_name=xOS \
    ro.xos.version=0.1.0-dev \
    ro.xos.account_network=pending

PRODUCT_PACKAGES += Xstore XonolithSystemCenter

PRODUCT_COPY_FILES += \
    vendor/extra/permissions/privapp-permissions-xonolithos.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-xonolithos.xml

# Generated from checksum- and certificate-verified local APKs by prepare-tree.py.
$(call inherit-product, vendor/extra/prebuilts/prebuilts.mk)

# No GApps, microG, signature spoofing, or invented Bitpost implementation.
# Retain the upstream keyboard and camera for first-boot recovery/fallback.
