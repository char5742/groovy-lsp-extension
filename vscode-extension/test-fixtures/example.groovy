// Test Groovy file to verify VSCode extension activation

package com.example

import spock.lang.Specification

interface UserService {
    User findById(Long id)
    void save(User user)
    boolean exists(String email)
}

class User {

    Long id
    String name
    String email
}

class UserController {
    private UserService userService
    
    UserController(UserService userService) {
        this.userService = userService
    }
    
    User getUser(Long id) {
        return userService.findById(id)
    }
    
    boolean createUser(String name, String email) {
        if (userService.exists(email)) {
            return false
        }
        
        def user = new User(name: name, email: email)
        userService.save(user)
        return true
    }
}

class MockingExampleSpec extends Specification {
    
    def userService = Mock(UserService)
    def controller = new UserController(userService)
    
    def "should get user by id using mock"() {
        given:
        def expectedUser = new User(id: 1L, name: "John", email: "john@example.com")
        
        when:
        def result = controller.getUser(1L)
        
        then:
        1 * userService.findById(1L) >> expectedUser
        result == expectedUser
        result.name == "John"
    }
    
    def "should create new user when email doesn't exist"() {
        given:
        def name = "Jane"
        def email = "jane@example.com"
        
        when:
        def success = controller.createUser(name, email)
        
        then:
        1 * userService.exists(email) >> false
        1 * userService.save({ User user ->
            user.name == name && user.email == email
        })
        success == true
    }
     
    def "should not create user when email already exists"() {
        given:
        def email = "existing@example.com"
        
        when:
        def success = controller.createUser("User", email)
        
        then:
        1 * userService.exists(email) >> true
        0 * userService.save(_)
        success == false
    }
    
    def "demonstrating interaction verification"() {
        when:
        controller.getUser(123L)
        
        then:
        1 * userService.findById(123L)
    }
    
    def "demonstrating stub with different return values"() {
        given:
        userService.exists(_) >>> [false, true, false]
        
        expect:
        controller.createUser("user1", "email1") == true
        controller.createUser("user2", "email2") == false
        controller.createUser("user3", "email3") == true
    }
    
    def "using argument matchers"() {
        when:
        controller.getUser(999L)
        
        then:
        1 * userService.findById({ it > 0 }) >> new User(id: 999L)
    }
}