// シンプルなパターンマッチングテスト
class SimplePatternMatch {
    static void test() {
        def value = "Hello"
        
        switch (value) {
            case String s:
                println "String: $s"
                break
            case Integer i:
                println "Integer: $i"
                break
            default:
                println "Other"
        }
    }
}