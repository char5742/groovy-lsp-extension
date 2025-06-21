// Test file for OOP features hover functionality

// レコードクラス（Groovy 4.0+）
record Person(String name, int age, String email) {
    // カスタムコンストラクタ
    Person(String name, int age) {
        this(name, age, "${name.toLowerCase()}@example.com")
    }
    
    // カスタムメソッド
    String getInfo() {
        return "${name} (${age})"
    }
}

// データクラス（@groovy.transform.Canonical）
import groovy.transform.Canonical

@Canonical
class Product {
    String name
    BigDecimal price
    int quantity
}

// 継承とオーバーライド
abstract class Animal {
    protected String name
    
    Animal(String name) {
        this.name = name
    }
    
    abstract String makeSound()
    
    String getName() {
        return name
    }
}

class Dog extends Animal {
    private String breed
    
    Dog(String name, String breed) {
        super(name)
        this.breed = breed
    }
    
    @Override
    String makeSound() {
        return "Woof!"
    }
    
    @Override
    String getName() {
        return "Dog: ${super.getName()}"
    }
}

// ネストクラス
class OuterClass {
    private String outerField = "outer"
    
    // 内部クラス（インナークラス）
    class InnerClass {
        String innerField = "inner"
        
        String accessOuter() {
            return outerField
        }
    }
    
    // 静的ネストクラス
    static class StaticNestedClass {
        static String staticField = "static nested"
        
        String getInfo() {
            return "I am a static nested class"
        }
    }
    
    // メソッド内のローカルクラス（Groovyではサポートされていないためコメントアウト）
    // def createLocalClass() {
    //     class LocalClass {
    //         String localField = "local"
    //     }
    //     return new LocalClass()
    // }
}

// 型パラメータ境界
class Container<T extends Number> {
    private T value
    
    Container(T value) {
        this.value = value
    }
    
    T getValue() {
        return value
    }
    
    void setValue(T newValue) {
        this.value = newValue
    }
}

// 複雑な型パラメータ
interface Processor<I, O> {
    O process(I input)
}

class StringToIntProcessor implements Processor<String, Integer> {
    @Override
    Integer process(String input) {
        return input.length()
    }
}

// オーバーロード
class Calculator {
    int add(int a, int b) {
        return a + b
    }
    
    double add(double a, double b) {
        return a + b
    }
    
    int add(int a, int b, int c) {
        return a + b + c
    }
}

// 使用例
class OOPExample {
    void demonstrateFeatures() {
        // レコード使用
        def person = new Person("John", 30)
        def personInfo = person.getInfo()
        
        // データクラス使用
        def product = new Product(name: "Laptop", price: 999.99, quantity: 5)
        
        // 継承とオーバーライド
        Animal dog = new Dog("Buddy", "Golden Retriever")
        String sound = dog.makeSound()
        String name = dog.getName()
        
        // ネストクラス
        def outer = new OuterClass()
        def inner = outer.new InnerClass()
        def staticNested = new OuterClass.StaticNestedClass()
        
        // 型パラメータ境界
        Container<Integer> intContainer = new Container<>(42)
        Container<Double> doubleContainer = new Container<>(3.14)
        
        // オーバーロード
        def calc = new Calculator()
        int sum1 = calc.add(1, 2)
        double sum2 = calc.add(1.5, 2.5)
        int sum3 = calc.add(1, 2, 3)
    }
}