import re

with open('/app/applet/app/src/main/java/com/example/data/network/SupabaseAuthService.kt', 'r') as f:
    content = f.read()

content = content.replace("import io.github.jan.supabase.gotrue.user.UserInfo", "import io.github.jan.supabase.gotrue.user.UserInfo\nimport io.github.jan.supabase.postgrest.postgrest")

with open('/app/applet/app/src/main/java/com/example/data/network/SupabaseAuthService.kt', 'w') as f:
    f.write(content)
