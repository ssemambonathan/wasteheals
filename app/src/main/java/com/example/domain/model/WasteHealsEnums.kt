package com.example.domain.model

enum class UserRoleKey(val key: String, val label: String) {
  PICKER("picker", "Picker"),
  COLLECTOR("collector", "Collector"),
  ADMIN("admin", "Admin"),
  HEALTH_PROVIDER("health_provider", "Health Provider");

  companion object {
    fun fromKey(raw: String?): UserRoleKey =
      entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: PICKER
  }
}

enum class UserRole(
  val title: String,
  val workspaceGroup: WorkspaceGroup,
  val isPrivileged: Boolean,
  val firestoreRoleKey: String
) {
  SUPER_ADMIN("Super Administrator", WorkspaceGroup.ADMIN_GOVERNANCE, true, UserRoleKey.ADMIN.key),
  SYSTEM_ADMIN("System Administrator", WorkspaceGroup.ADMIN_GOVERNANCE, true, UserRoleKey.ADMIN.key),
  FINANCE_MANAGER("Finance Manager", WorkspaceGroup.FINANCE_TREASURY, true, UserRoleKey.ADMIN.key),
  FINANCE_OFFICER("Finance Officer", WorkspaceGroup.FINANCE_TREASURY, true, UserRoleKey.ADMIN.key),
  TREASURY_OFFICER("Treasury Officer", WorkspaceGroup.FINANCE_TREASURY, true, UserRoleKey.ADMIN.key),
  AUDITOR("Auditor", WorkspaceGroup.FINANCE_TREASURY, true, UserRoleKey.ADMIN.key),
  OPERATIONS_MANAGER("Operations Manager", WorkspaceGroup.HUB_OPERATIONS, true, UserRoleKey.ADMIN.key),
  HUB_MANAGER("Hub Manager", WorkspaceGroup.HUB_OPERATIONS, true, UserRoleKey.ADMIN.key),
  COLLECTION_AGENT("Collection Agent", WorkspaceGroup.FIELD_COLLECTION, false, UserRoleKey.COLLECTOR.key),
  PICKER("Waste Picker", WorkspaceGroup.PICKER_PORTAL, false, UserRoleKey.PICKER.key),
  HOUSEHOLD("Household Member", WorkspaceGroup.HOUSEHOLD_PORTAL, false, UserRoleKey.PICKER.key),
  BUYER("Recycling / Compost Buyer", WorkspaceGroup.BUYER_MARKETPLACE, false, UserRoleKey.PICKER.key),
  ORGANIC_OPERATOR("Organic Processing Operator", WorkspaceGroup.HUB_OPERATIONS, true, UserRoleKey.COLLECTOR.key),
  HEALTHCARE_MANAGER("Healthcare Manager", WorkspaceGroup.HEALTHCARE_CLINIC, true, UserRoleKey.HEALTH_PROVIDER.key),
  CLINIC_USER("Healthcare Clinic User", WorkspaceGroup.HEALTHCARE_CLINIC, false, UserRoleKey.HEALTH_PROVIDER.key);

  companion object {
    fun fromString(raw: String?): UserRole =
      entries.firstOrNull { it.name == raw } ?: when (raw?.trim()?.lowercase()) {
        UserRoleKey.ADMIN.key -> SUPER_ADMIN
        UserRoleKey.COLLECTOR.key -> COLLECTION_AGENT
        UserRoleKey.HEALTH_PROVIDER.key -> HEALTHCARE_MANAGER
        else -> PICKER
      }
  }
}

enum class WorkspaceGroup(val label: String) {
  ADMIN_GOVERNANCE("Admin & Governance"),
  FINANCE_TREASURY("Finance & Treasury"),
  HUB_OPERATIONS("Hub & Composting"),
  FIELD_COLLECTION("Field Collection"),
  PICKER_PORTAL("Picker Portal"),
  HOUSEHOLD_PORTAL("Household Portal"),
  BUYER_MARKETPLACE("Buyer Marketplace"),
  HEALTHCARE_CLINIC("Healthcare & Clinics")
}

enum class AccountStatus {
  ACTIVE, PENDING_VERIFICATION, SUSPENDED;
  companion object {
    fun fromString(raw: String?): AccountStatus = entries.firstOrNull { it.name == raw } ?: ACTIVE
  }
}

enum class MaterialCategory {
  RECYCLABLE, ORGANIC, RESIDUAL;
  companion object {
    fun fromString(raw: String?): MaterialCategory = entries.firstOrNull { it.name == raw } ?: RECYCLABLE
  }
}

enum class OriginType(val label: String) {
  PICKER_HUB("Picker Hub Delivery"),
  HOUSEHOLD_AGENT("Household Agent Pickup"),
  COMMUNITY_EVENT("Community Collection Event");
  companion object {
    fun fromString(raw: String?): OriginType = entries.firstOrNull { it.name == raw } ?: PICKER_HUB
  }
}

enum class VerificationStatus {
  PENDING_VERIFICATION, ACCEPTED, REJECTED, DISPUTED;
  companion object {
    fun fromString(raw: String?): VerificationStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING_VERIFICATION
  }
}

enum class SyncStatus {
  PENDING_SYNC, SYNCING, SYNCED, CONFLICT_DUPLICATE, SYNC_FAILED
}

enum class SettlementMethod {
  DIRECT_MOMO, WALLET_DISABLED;
  companion object {
    fun fromString(raw: String?): SettlementMethod =
      entries.firstOrNull { it.name == raw } ?: DIRECT_MOMO
  }
}

enum class PayoutStatus {
  UNSETTLED, QUEUED, PROCESSING, SETTLED, FAILED, UNKNOWN_RECONCILE;
  companion object {
    fun fromString(raw: String?): PayoutStatus = entries.firstOrNull { it.name == raw } ?: UNSETTLED
  }
}

enum class MoMoProvider(val displayName: String) {
  MTN_MOMO("MTN Mobile Money"),
  AIRTEL_MONEY("Airtel Money");
  companion object {
    fun fromString(raw: String?): MoMoProvider = entries.firstOrNull { it.name == raw } ?: MTN_MOMO
  }
}

enum class MoMoLifecycleStatus {
  CREATED, VALIDATING, SUBMITTED, PENDING, SUCCESS, FAILED, UNKNOWN;
  companion object {
    fun fromString(raw: String?): MoMoLifecycleStatus =
      entries.firstOrNull { it.name == raw } ?: CREATED
  }
}

enum class ReconciliationStatus {
  UNRECONCILED, MATCHED, UNRESOLVED, REVERSED;
  companion object {
    fun fromString(raw: String?): ReconciliationStatus =
      entries.firstOrNull { it.name == raw } ?: UNRECONCILED
  }
}

enum class SaleStatus {
  ORDERED, DISPATCHED, CONFIRMED, PARTIALLY_PAID, PAID, REJECTED;
  companion object {
    fun fromString(raw: String?): SaleStatus = entries.firstOrNull { it.name == raw } ?: ORDERED
  }
}

enum class CompostStatus {
  ACTIVE, CURING, QUALITY_CHECK, APPROVED, SOLD_OUT;
  companion object {
    fun fromString(raw: String?): CompostStatus = entries.firstOrNull { it.name == raw } ?: ACTIVE
  }
}

enum class VoucherStatus {
  ISSUED, REDEEMED, EXPIRED, CANCELLED;
  companion object {
    fun fromString(raw: String?): VoucherStatus = entries.firstOrNull { it.name == raw } ?: ISSUED
  }
}

enum class ClaimStatus {
  SUBMITTED, APPROVED, REJECTED, PAID;
  companion object {
    fun fromString(raw: String?): ClaimStatus = entries.firstOrNull { it.name == raw } ?: SUBMITTED
  }
}

enum class DisputeStatus {
  OPEN, INVESTIGATING, RESOLVED, APPEALED, CLOSED;
  companion object {
    fun fromString(raw: String?): DisputeStatus = entries.firstOrNull { it.name == raw } ?: OPEN
  }
}

enum class ApprovalStatus {
  PENDING, APPROVED, REJECTED;
  companion object {
    fun fromString(raw: String?): ApprovalStatus = entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

enum class AccountCategory {
  ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, RESTRICTED_FUND;
  companion object {
    fun fromString(raw: String?): AccountCategory = entries.firstOrNull { it.name == raw } ?: ASSET
  }
}
