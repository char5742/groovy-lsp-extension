// Groovyのパターンマッチング機能のテストファイル

class PatternMatchTest {
    
    // 基本的なswitch文
    static void basicSwitch() {
        def value = 42
        
        switch (value) {
            case 1:
                println "One"
                break
            case 42:
                println "The answer"
                break
            case 100:
                println "Century"
                break
            default:
                println "Unknown"
        }
    }
    
    // 型チェックを使ったパターンマッチング
    static void typePatternMatching() {
        def items = ["Hello", 42, 3.14, true, [1, 2, 3], null]
        
        items.each { item ->
            switch (item) {
                case String s:
                    println "String: $s (length: ${s.length()})"
                    break
                case Integer i:
                    println "Integer: $i (doubled: ${i * 2})"
                    break
                case Double d:
                    println "Double: $d (rounded: ${Math.round(d)})"
                    break
                case Boolean b:
                    println "Boolean: $b (negated: ${!b})"
                    break
                case List list:
                    println "List: $list (size: ${list.size()})"
                    break
                case null:
                    println "Null value"
                    break
                default:
                    println "Unknown type: ${item.class}"
            }
        }
    }
    
    // コレクションを使ったパターンマッチング
    static void collectionPatternMatching() {
        def collections = [
            [],
            [1],
            [1, 2],
            [1, 2, 3],
            "abc",
            1..5
        ]
        
        collections.each { coll ->
            switch (coll) {
                case []:
                    println "Empty list"
                    break
                case [_]:
                    println "Single element list: $coll"
                    break
                case [_, _]:
                    println "Two element list: $coll"
                    break
                case List:
                    println "List with ${coll.size()} elements: $coll"
                    break
                case String:
                    println "String as collection: $coll"
                    break
                case Range:
                    println "Range: $coll"
                    break
                default:
                    println "Unknown collection type"
            }
        }
    }
    
    // 範囲を使ったパターンマッチング
    static void rangePatternMatching() {
        def scores = [0, 50, 75, 90, 100]
        
        scores.each { score ->
            switch (score) {
                case 0..<50:
                    println "$score: Failed"
                    break
                case 50..<70:
                    println "$score: Pass"
                    break
                case 70..<90:
                    println "$score: Good"
                    break
                case 90..100:
                    println "$score: Excellent"
                    break
                default:
                    println "$score: Invalid score"
            }
        }
    }
    
    // 正規表現を使ったパターンマッチング
    static void regexPatternMatching() {
        def strings = ["hello", "Hello", "HELLO", "123", "test@example.com"]
        
        strings.each { str ->
            switch (str) {
                case ~/^[a-z]+$/:
                    println "$str: All lowercase"
                    break
                case ~/^[A-Z]+$/:
                    println "$str: All uppercase"
                    break
                case ~/^[A-Z][a-z]+$/:
                    println "$str: Title case"
                    break
                case ~/^\d+$/:
                    println "$str: All digits"
                    break
                case ~/.+@.+\..+/:
                    println "$str: Email format"
                    break
                default:
                    println "$str: Other format"
            }
        }
    }
    
    // クロージャを使ったパターンマッチング
    static void closurePatternMatching() {
        def numbers = [-5, 0, 3, 10, 15, 20]
        
        numbers.each { num ->
            switch (num) {
                case { it < 0 }:
                    println "$num: Negative"
                    break
                case { it == 0 }:
                    println "$num: Zero"
                    break
                case { it > 0 && it < 10 }:
                    println "$num: Single digit positive"
                    break
                case { it >= 10 && it < 20 }:
                    println "$num: Teen"
                    break
                case { it >= 20 }:
                    println "$num: Twenty or more"
                    break
            }
        }
    }
    
    // 複合的なパターンマッチング
    static void complexPatternMatching() {
        def data = [
            [type: "user", name: "Alice", age: 25],
            [type: "user", name: "Bob", age: 17],
            [type: "admin", name: "Charlie", level: 5],
            [type: "guest", session: "123456"],
            "invalid data"
        ]
        
        data.each { item ->
            switch (item) {
                case Map m:
                    switch (m.type) {
                        case "user":
                            if (m.age >= 18) {
                                println "Adult user: ${m.name} (${m.age})"
                            } else {
                                println "Minor user: ${m.name} (${m.age})"
                            }
                            break
                        case "admin":
                            println "Admin: ${m.name} (level ${m.level})"
                            break
                        case "guest":
                            println "Guest session: ${m.session}"
                            break
                        default:
                            println "Unknown user type"
                    }
                    break
                case String:
                    println "Invalid data format: $item"
                    break
                default:
                    println "Unexpected data type"
            }
        }
    }
    
    // インスタンスofを使ったパターンマッチング
    static void instanceOfPatternMatching() {
        def objects = [
            new Date(),
            new ArrayList<>(),
            new HashMap<>(),
            new StringBuilder("test"),
            42,
            "string"
        ]
        
        objects.each { obj ->
            switch (obj) {
                case Date:
                    println "Date object: $obj"
                    break
                case List:
                    println "List implementation: ${obj.class.simpleName}"
                    break
                case Map:
                    println "Map implementation: ${obj.class.simpleName}"
                    break
                case CharSequence:
                    println "CharSequence: $obj"
                    break
                case Number:
                    println "Number: $obj"
                    break
                default:
                    println "Other type: ${obj.class}"
            }
        }
    }
    
    // メインメソッド
    static void main(String[] args) {
        println "=== 基本的なswitch文 ==="
        basicSwitch()
        
        println "\n=== 型チェックパターンマッチング ==="
        typePatternMatching()
        
        println "\n=== コレクションパターンマッチング ==="
        collectionPatternMatching()
        
        println "\n=== 範囲パターンマッチング ==="
        rangePatternMatching()
        
        println "\n=== 正規表現パターンマッチング ==="
        regexPatternMatching()
        
        println "\n=== クロージャパターンマッチング ==="
        closurePatternMatching()
        
        println "\n=== 複合的なパターンマッチング ==="
        complexPatternMatching()
        
        println "\n=== instanceOfパターンマッチング ==="
        instanceOfPatternMatching()
    }
}

// テスト実行
PatternMatchTest.main()