// Groovyのデストラクチャリング（分割代入）テスト
class DestructuringTest {
    
    static void testListDestructuring() {
        // リストからの分割代入
        def list = [1, 2, 3, 4, 5]
        
        // 基本的な分割代入
        def (a, b) = list
        println "a = $a, b = $b"  // a = 1, b = 2
        
        // 3つの要素を取得
        def (x, y, z) = ['foo', 'bar', 'baz']
        println "x = $x, y = $y, z = $z"
        
        // 残りの要素を無視
        def (first, second) = [10, 20, 30, 40]
        println "first = $first, second = $second"
    }
    
    static void testMultipleAssignment() {
        // 複数代入の例
        def (String name, int age) = ['Alice', 25]
        println "Name: $name, Age: $age"
        
        // メソッドの戻り値から
        def (status, message) = getStatusAndMessage()
        println "Status: $status, Message: $message"
    }
    
    static List getStatusAndMessage() {
        return [200, "OK"]
    }
    
    static void testMapDestructuring() {
        // マップの分割代入（Groovyではキーを指定）
        def map = [name: 'Bob', age: 30, city: 'Tokyo']
        
        // 特定のキーの値を取得
        def name = map.name
        def age = map.age
        println "Name: $name, Age: $age"
    }
    
    static void testNestedDestructuring() {
        // ネストした構造の分割代入
        def data = [[1, 2], [3, 4], [5, 6]]
        
        // 最初の要素を分割
        def (first, second) = data[0]
        println "First pair: $first, $second"
        
        // ループ内での使用
        data.each { pair ->
            def (left, right) = pair
            println "Left: $left, Right: $right"
        }
    }
    
    static void main(String[] args) {
        println "=== リストの分割代入 ==="
        testListDestructuring()
        
        println "\n=== 複数代入 ==="
        testMultipleAssignment()
        
        println "\n=== マップの分割代入 ==="
        testMapDestructuring()
        
        println "\n=== ネストした分割代入 ==="
        testNestedDestructuring()
    }
}