package com.github.harrydimas.test;

import com.github.harrydimas.leetcode_solution.LeetcodeSolutionApplication;
import com.github.harrydimas.leetcode_solution.user.domain.User;
import com.github.harrydimas.leetcode_solution.user.repos.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LeetcodeSolutionApplication.class)
public class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateUser() {
        User user = new User();
        user.setUsername("john_doe");
        user.setPassword("john_doe123");
        user.setFirstName("John");
        user.setLastName("Doe");
        userRepository.save(user);

        User found = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(user.getUsername(), found.getUsername());
        assertEquals(user.getPassword(), found.getPassword());
        assertEquals(user.getFirstName(), found.getFirstName());
        assertEquals(user.getLastName(), found.getLastName());
        assertEquals(0, found.getVersion());
    }

    @Test
    public void testUpdateUserError() {
        User user = new User();
        user.setUsername("john_doe");
        user.setPassword("john_doe123");
        user.setFirstName("John");
        user.setLastName("Doe");
        userRepository.save(user);

        User found1 = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(found1);

        User found2 = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(found2);

        found1.setFirstName("Johnny");
        found1 = userRepository.save(found1);
        assertEquals("Johnny", found1.getFirstName());

        found2.setFirstName("John");
        var error = assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                userRepository.save(found2)
        );
        System.out.println(error.getMessage());
    }
}
