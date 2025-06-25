// 標準的なGroovyのパターンマッチング
class StandardPatternMatch {
    
    static void testTypeChecking() {
        def items = ["Hello", 42, 3.14, true, [1, 2, 3], null]
        
        items.each { item ->
            switch (item) {
                case String:
                    println "String: $item (length: ${item.length()})"
                    break
                case Integer:
                    println "Integer: $item (doubled: ${item * 2})"
                    break
                case Double:
                    println "Double: $item (rounded: ${Math.round(item)})"
                    break
                case Boolean:
                    println "Boolean: $item (negated: ${!item})"
                    break
                case List:
                    println "List: $item (size: ${item.size()})"
                    break
                case null:
                    println "Null value"
                    break
                default:
                    println "Unknown type: ${item.class}"
            }
        }
    }
    
    static void testInstanceOf() {
        def value = "Hello"
        
        if (value instanceof String) {
            String s = value  // 明示的なキャスト
            println "String length: ${s.length()}"
        }
    }
}