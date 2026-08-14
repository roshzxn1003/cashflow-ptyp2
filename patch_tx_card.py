import re

with open('/app/applet/app/src/main/java/com/example/ui/components/TransactionItemCard.kt', 'r') as f:
    content = f.read()

sig = """fun TransactionItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    creatorName: String? = null
) {"""

content = re.sub(r'fun TransactionItemCard\([\s\S]*?modifier: Modifier = Modifier\n\) \{', sig, content)

content = content.replace("import com.example.data.models.TransactionType", "import com.example.data.models.TransactionType\nimport com.example.data.models.FinanceScope")

middle = """                            Text(
                                text = transaction.category,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (transaction.financeScope == FinanceScope.FAMILY && creatorName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Added by $creatorName",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }"""

content = re.sub(r'                            Text\(\s*text = transaction\.category,\s*fontSize = 12\.sp,\s*color = MaterialTheme\.colorScheme\.onSurfaceVariant\s*\)\s*\}\s*\}', middle, content)

with open('/app/applet/app/src/main/java/com/example/ui/components/TransactionItemCard.kt', 'w') as f:
    f.write(content)
