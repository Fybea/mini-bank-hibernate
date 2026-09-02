package sorokin.java.course.user;

import jakarta.persistence.*;
import sorokin.java.course.account.Account;

import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "login", unique = true)
    private String login;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Account> accountList;

    public User(String login, List<Account> accountList) {
        this.login = login;
        this.accountList = accountList;
    }

    public User() {
    }

    public int getId() {
        return id;
    }

    public List<Account> getAccountList() {
        return accountList;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", accountList=" + accountList +
                '}';
    }
}
