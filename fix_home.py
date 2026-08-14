import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('"Manage your family\'s income,\\\\nexpenses, budgets and savings\\\\ntogether.",', '"Manage your family\'s income,\\nexpenses, budgets and savings\\ntogether.",')
content = content.replace('"Manage your family\'s income,\\nexpenses, budgets and savings\\ntogether.",', '"Manage your family\'s income,\\nexpenses, budgets and savings\\ntogether.",')

# Let's just use string concatenation to be safe
safe_string = '"Manage your family\'s income,\\n" + "expenses, budgets and savings\\n" + "together."'
content = re.sub(r'"Manage your family\'s income.*?together.",', safe_string + ',', content, flags=re.DOTALL)

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
