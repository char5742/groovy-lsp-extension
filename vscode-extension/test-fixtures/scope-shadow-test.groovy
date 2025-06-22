// Groovyのスコープシャドウイングテスト
class ScopeShadowTest {
    
    // クラスレベルのフィールド
    static String name = "Class level"
    static int count = 100
    
    // インスタンスフィールド
    String instanceName = "Instance level"
    
    static void testMethodShadowing() {
        // メソッドレベルのローカル変数（クラスフィールドをシャドウ）
        String name = "Method level"
        int count = 200
        
        println "Method scope - name: $name, count: $count"
        
        // ブロック内でさらにシャドウ
        if (true) {
            String name = "Block level"  // メソッドレベルの変数をシャドウ
            println "Block scope - name: $name, count: $count"  // countはメソッドレベルの値
        }
        
        // メソッドレベルに戻る
        println "Back to method scope - name: $name"
    }
    
    static void testClosureShadowing() {
        String outer = "Outer scope"
        int value = 1
        
        // クロージャ内でのシャドウイング
        def closure = {
            String outer = "Closure scope"  // 外側の変数をシャドウ
            println "Inside closure - outer: $outer, value: $value"  // valueは外側の値
            
            // ネストしたクロージャ
            def nested = {
                String outer = "Nested closure"  // クロージャの変数をシャドウ
                int value = 2  // 外側の変数をシャドウ
                println "Nested closure - outer: $outer, value: $value"
            }
            nested()
        }
        
        closure()
        println "Outside closure - outer: $outer, value: $value"
    }
    
    void testInstanceMethodShadowing() {
        // インスタンスフィールドをシャドウ
        String instanceName = "Local in instance method"
        println "Instance method - instanceName: $instanceName"
        
        // thisを使ってインスタンスフィールドにアクセス
        println "Instance field via this - instanceName: ${this.instanceName}"
    }
    
    static void testLoopShadowing() {
        String item = "Outside loop"
        
        def list = ['A', 'B', 'C']
        list.each { item ->  // ループ変数が外側の変数をシャドウ
            println "Loop item: $item"
        }
        
        println "After loop - item: $item"  // 外側の値は変わらない
    }
    
    static void main(String[] args) {
        println "=== メソッドレベルのシャドウイング ==="
        testMethodShadowing()
        
        println "\n=== クロージャのシャドウイング ==="
        testClosureShadowing()
        
        println "\n=== インスタンスメソッドのシャドウイング ==="
        new ScopeShadowTest().testInstanceMethodShadowing()
        
        println "\n=== ループでのシャドウイング ==="
        testLoopShadowing()
        
        println "\n=== クラスレベルの値 ==="
        println "Class level - name: $name, count: $count"
    }
}