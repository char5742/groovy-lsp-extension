// Test Groovy file to verify VSCode extension activation

class HelloWorld {
    static void main(String[] args) {
        println "Hello, Groovy LSP!"
        
        def list = [1, 2, 3, 4, 5]
        list.each { item ->
            println "Item: $item"
        }
    }
}

// Spock test example
import spock.lang.Specification

class ExampleSpec extends Specification {
    def "should demonstrate Spock test"() {
        given:
        def a = 5
        def b = 10
        
        when:
        def result = a + b
        
        then:
        result == 15
    }
}