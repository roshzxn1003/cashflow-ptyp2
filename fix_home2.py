import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

# Let's replace the Text("Manage your family's income...") block completely
text_block_pattern = r'Text\(\s*"Manage your family.*?together\.".*?\)'
new_text_block = """Text(
                        "Manage your family's income,\\nexpenses, budgets and savings\\ntogether.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )"""

content = re.sub(r'Text\(\s*"Manage your family.*?\n.*?\n.*?together\.".*?\)', new_text_block, content, flags=re.DOTALL)
# Try more relaxed pattern if it fails
content = re.sub(r'Text\(\s*"Manage your family.*?textAlign = androidx.compose.ui.text.style.TextAlign.Center\s*\)', new_text_block, content, flags=re.DOTALL)

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
