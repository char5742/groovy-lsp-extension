// Test file for static method and qualified call hover functionality

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 完全修飾名でのメソッド呼び出し
class TimeExample {
    void demonstrateQualifiedCalls() {
        // Qualified呼び出し
        def now = java.time.Instant.now()
        def today = java.time.LocalDate.now()
        def dateTime = java.time.LocalDateTime.now()
        
        // 静的メソッド呼び出し
        double sinValue = Math.sin(0.5)
        double cosValue = Math.cos(0.5)
        double sqrtValue = Math.sqrt(16)
        
        // インポートを使った静的メソッド呼び出し
        def instant = Instant.now()
        def date = LocalDate.now()
        def localDateTime = LocalDateTime.now()
        
        // 静的メソッドのチェーン呼び出し
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Systemクラスの静的メソッド
        long currentTime = System.currentTimeMillis()
        String property = System.getProperty("java.version")
        
        // Collectionsの静的メソッド
        def emptyList = Collections.emptyList()
        def singletonList = Collections.singletonList("item")
        
        // Arraysの静的メソッド
        int[] numbers = [3, 1, 4, 1, 5]
        Arrays.sort(numbers)
        String arrayString = Arrays.toString(numbers)
    }
    
    // 静的メソッドの定義
    static String staticMethod(String input) {
        return input.toUpperCase()
    }
    
    // 静的フィールド
    static final double PI_VALUE = Math.PI
    static final double E_VALUE = Math.E
}

// import aliasのテスト用（別ファイルが良いかも）
import java.time.LocalDate as LD
import java.time.LocalDateTime as LDT

class AliasExample {
    void useAliases() {
        def date = LD.now()
        def dateTime = LDT.now()
        
        // aliasを使った型宣言
        LD birthday = LD.of(2000, 1, 1)
        LDT appointment = LDT.of(2024, 6, 21, 15, 30)
    }
}