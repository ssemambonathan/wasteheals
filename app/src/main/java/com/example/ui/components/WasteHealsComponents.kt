package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.model.UserRole
import com.example.domain.model.WasteDelivery
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularMoneyStyle

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Surface(
          color = accentColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.size(30.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = accentColor,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
      Text(
        text = value,
        style = TabularMoneyStyle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun RoleWorkspaceChip(
  activeRole: UserRole,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    shape = RoundedCornerShape(999.dp),
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clickable(onClick = onClick)
      .testTag("role_switcher_chip")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Badge,
        contentDescription = "Active role",
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = activeRole.title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Icon(
        imageVector = Icons.Filled.SwapHoriz,
        contentDescription = "Switch workspace role",
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSwitcherSheet(
  assignedRoles: List<UserRole>,
  activeRole: UserRole,
  onSelectRole: (UserRole) -> Unit,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Switch Role Workspace",
            style = MaterialTheme.typography.headlineSmall
          )
          Text(
            text = "Only roles assigned to your account by RBAC are shown.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close role switcher")
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(assignedRoles) { role ->
          val selected = role == activeRole
          Surface(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              1.dp,
              if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelectRole(role)
                onDismiss()
              }
              .testTag("role_option_${role.name}")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(role.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = role.workspaceGroup.label,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (selected) {
                Icon(
                  Icons.Filled.CheckCircle,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
fun DigitalReceiptDialog(
  delivery: WasteDelivery,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
    title = { Text("WasteHeals Official Receipt") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ReceiptLine("Receipt Ref", delivery.receiptRef)
        ReceiptLine("Picker / Source", delivery.pickerName)
        ReceiptLine("Material & Grade", "${delivery.materialName} (${delivery.grade})")
        ReceiptLine("Gross Weight", MoneyAndWeight.formatKg(delivery.grossKgMilli))
        ReceiptLine("Tare Weight", MoneyAndWeight.formatKg(delivery.tareKgMilli))
        ReceiptLine("Net Weight", MoneyAndWeight.formatKg(delivery.netKgMilli))
        ReceiptLine("Quality Deduction", MoneyAndWeight.formatKg(delivery.deductionKgMilli))
        ReceiptLine("Accepted Weight", MoneyAndWeight.formatKg(delivery.acceptedKgMilli))
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        ReceiptLine("Locked Rate", "${MoneyAndWeight.formatUgx(delivery.unitPriceUgxMinor)}/kg")
        ReceiptLine("Guaranteed Earning", MoneyAndWeight.formatUgx(delivery.totalEarningsUgxMinor))
        ReceiptLine("Status", delivery.verificationStatus)
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = TabularCompactStyle, color = MaterialTheme.colorScheme.onSurface)
  }
}
