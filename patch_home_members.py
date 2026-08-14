import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

sig_old = """    onOpenCreateFamily: () -> Unit,
    onOpenAddTransaction: () -> Unit,"""
sig_new = """    onOpenCreateFamily: () -> Unit,
    onManageMembers: () -> Unit,
    onOpenAddTransaction: () -> Unit,"""
content = content.replace(sig_old, sig_new)

# Add members button inside "Family Overview" header
welcome_header_old = """                    Column {
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Finance" else "Hello, Finance Master! 👋",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Overview" else "CashFlow Overview",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface("""

welcome_header_new = """                    Column {
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Finance" else "Hello, Finance Master! 👋",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Overview" else "CashFlow Overview",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (currentFinanceScope == FinanceScope.FAMILY) {
                        TextButton(onClick = onManageMembers) {
                            Text("Members")
                        }
                    }

                    Surface("""

content = content.replace(welcome_header_old, welcome_header_new)

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
