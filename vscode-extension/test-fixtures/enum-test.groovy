// Test file for enum hover functionality

enum Color {
    RED("FF0000"),
    GREEN("00FF00"),
    BLUE("0000FF")
    
    private final String hex
    
    Color(String hex) {
        this.hex = hex
    }
    
    String getHex() {
        return hex
    }
}

enum Status {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

class EnumExample {
    Color favoriteColor = Color.RED
    Status currentStatus = Status.PENDING
    
    void processWithColor(Color color) {
        println "Processing with ${color.name()} (${color.hex})"
    }
    
    void updateStatus(Status newStatus) {
        this.currentStatus = newStatus
    }
    
    void example() {
        // enum定数への参照
        def red = Color.RED
        def pending = Status.PENDING
        
        // enumメソッドの呼び出し
        String hexValue = red.getHex()
        String colorName = red.name()
        
        // switch文でのenum使用
        switch (currentStatus) {
            case Status.PENDING:
                println "Waiting..."
                break
            case Status.PROCESSING:
                println "In progress..."
                break
            case Status.COMPLETED:
                println "Done!"
                break
            case Status.FAILED:
                println "Error occurred"
                break
        }
    }
}

// ネストされたenum
class OuterClass {
    enum InnerEnum {
        OPTION_A,
        OPTION_B,
        OPTION_C
    }
    
    InnerEnum selectedOption = InnerEnum.OPTION_A
}