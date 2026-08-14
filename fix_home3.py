import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('"Manage your family\'s income,\nexpenses, budgets and savings\ntogether.",', '"""Manage your family\'s income,\nexpenses, budgets and savings\ntogether.""",')

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
