import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

profile_old = """                    4 -> ProfileScreen(
                        state = uiState,
                        onCurrencySelect = { curr -> viewModel.setCurrency(curr) },
                        onDeleteScannedItem = { item -> viewModel.deleteScannedItem(item) }
                    )"""

profile_new = """                    4 -> ProfileScreen(
                        state = uiState,
                        viewModel = viewModel,
                        onCurrencySelect = { curr -> viewModel.setCurrency(curr) },
                        onDeleteScannedItem = { item -> viewModel.deleteScannedItem(item) }
                    )"""

content = content.replace(profile_old, profile_new)

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
