// Test file for closure 'it' variable type inference

// 基本的なitの使用
def numbers = [1, 2, 3, 4, 5]
numbers.each { 
    println it  // itはInteger型
}

// Stringリストでのit
def strings = ["apple", "banana", "cherry"]
strings.each { 
    println it.toUpperCase()  // itはString型
}

// マップでのit（MapEntry）
def map = [name: "John", age: 30]
map.each { 
    println "${it.key}: ${it.value}"  // itはMap.Entry型
}

// findAllでのit
def evenNumbers = numbers.findAll { 
    it % 2 == 0  // itはInteger型
}

// collectでのit
def doubled = numbers.collect { 
    it * 2  // itはInteger型
}

// カスタムオブジェクトでのit
class Person {
    String name
    int age
}

def people = [
    new Person(name: "Alice", age: 25),
    new Person(name: "Bob", age: 30)
]

people.each { 
    println it.name  // itはPerson型
}

// ネストされたクロージャ
def matrix = [[1, 2], [3, 4], [5, 6]]
matrix.each { row ->
    row.each { 
        println it  // itはInteger型（内側のクロージャ）
    }
}

// withメソッドでのit
def person = new Person(name: "Charlie", age: 35)
person.with {
    // withの中ではitは使用されない（thisがPersonを指す）
    println name  // thisのプロパティに直接アクセス
}

// timesメソッドでのit
5.times { 
    println "Count: $it"  // itはInteger型（0から4）
}

// ファイル処理でのit
// new File("test.txt").eachLine { 
//     println it  // itはString型（各行）
// }

// Groovy特有のコレクション操作
def result = [1, 2, 3, 4, 5].inject(0) { acc, val ->
    acc + val  // ここではitは使用されない（明示的パラメータ）
}

// groupByでのit
def grouped = strings.groupBy { 
    it.length()  // itはString型
}

// anyとeveryでのit
def hasLongString = strings.any { 
    it.length() > 5  // itはString型
}

def allShort = strings.every { 
    it.length() < 10  // itはString型
}