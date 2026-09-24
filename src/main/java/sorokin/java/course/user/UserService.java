package sorokin.java.course.user;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;
import sorokin.java.course.TransactionHelper;
import sorokin.java.course.account.Account;
import sorokin.java.course.account.AccountProperties;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserService {

    private final SessionFactory sessionFactory;
    private final TransactionHelper transactionHelper;
    private final AccountProperties accountProperties;

    public UserService(SessionFactory sessionFactory, TransactionHelper transactionHelper, AccountProperties accountProperties) {
        this.sessionFactory = sessionFactory;
        this.transactionHelper = transactionHelper;
        this.accountProperties = accountProperties;
    }

    public User createUser(String login) {
        String normalizedLogin = validateLogin(login);
        if (findUserByLogin(normalizedLogin) != null) {
            throw new IllegalArgumentException("Login already exists");
        }
        return transactionHelper.executeInTransaction(session -> {
            var user = new User(normalizedLogin, new ArrayList<>());
            Account account = new Account(user, accountProperties.getDefaultAmount());
            user.getAccountList().add(account);
            session.persist(user);
            session.persist(account);
            return user;
        });
    }

    public User findUserById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("user id must be > 0");
        }
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, id);

            if (user == null) {
                throw new IllegalArgumentException("No such user with id=%s".formatted(id));
            }
            return user;
        }

    }

    public User findUserByLogin(String login) {
        if (login == null || login.isEmpty()) {
            throw new IllegalArgumentException("user login must not be empty");
        }
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User u WHERE u.login = :login", User.class)
                    .setParameter("login", login).uniqueResult();
        }
    }

    public List<User> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("SELECT u FROM User u", User.class).list();
        }
    }

    private String validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login must not be blank");
        }
        return login.trim();
    }
}
