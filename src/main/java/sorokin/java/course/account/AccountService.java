package sorokin.java.course.account;

import org.hibernate.Session;
import org.springframework.stereotype.Component;
import sorokin.java.course.TransactionHelper;
import sorokin.java.course.user.User;

import java.util.List;
import java.util.Optional;

@Component
public class AccountService {

    private final AccountProperties accountProperties;
    private final TransactionHelper transactionHelper;

    public AccountService(AccountProperties accountProperties, TransactionHelper transactionHelper) {
        this.transactionHelper = transactionHelper;
        this.accountProperties = accountProperties;
    }

    public Account createAccount(User user) {
        return transactionHelper.executeInTransaction(session -> {
                    if (user == null) {
                        throw new IllegalArgumentException("user must not be null");
                    }
                    Account newAccount = new Account(user, accountProperties.getDefaultAmount());
                    session.merge(newAccount);
                    return newAccount;
                }
        );

    }

    public Optional<Account> findAccountById(Session session, Integer id) {
        validatePositiveId(id, "account id");
        return Optional.ofNullable(session.get(Account.class, id));
    }

    public List<Account> getUserAccounts(Session session, Integer userId) {
        return session.createQuery("SELECT U FROM Account U WHERE user.id=(:userId)", Account.class)
                .setParameter("userId", userId)
                .list();

    }

    public void withdraw(Integer fromAccountId, Integer amount) {
        validatePositiveId(fromAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {
            Account account = session.find(Account.class, fromAccountId);

            if (account == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));
            }
            if (amount > account.getMoneyAmount()) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted withdraw=%s"
                                .formatted(account.getId(), account.getMoneyAmount(), amount));
            }
            account.setMoneyAmount(account.getMoneyAmount() - amount);
        });
    }

    public void deposit(Integer toAccountId, Integer amount) {
        validatePositiveId(toAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {
            Account account = session.find(Account.class, toAccountId);
            if (account == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));
            }
            account.setMoneyAmount(account.getMoneyAmount() + amount);
        });
    }

    public Account closeAccount(Integer accountId) {
        validatePositiveId(accountId, "account id");
        return transactionHelper.executeInTransaction(session -> {
            Account accountToClose = session.find(Account.class, accountId);
            if (accountToClose == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(accountId));
            }
            var userId = accountToClose.getUser().getId();
            var userAccounts = getUserAccounts(session, userId);
            if (userAccounts.size() == 1) {
                throw new IllegalStateException("Can't close the only one account");
            }
            var accountToTransferMoney = userAccounts.stream()
                    .filter(it -> it.getId() != accountId)
                    .findFirst()
                    .orElseThrow();

            var newAmount = accountToTransferMoney.getMoneyAmount() + accountToClose.getMoneyAmount();
            accountToTransferMoney.setMoneyAmount(newAmount);
            User user = accountToClose.getUser();
            user.getAccountList().remove(accountToClose);
            session.remove(accountToClose);
            return accountToClose;
        });
    }

    public void transfer(int fromAccountId, int toAccountId, int amount) {
        transactionHelper.executeInTransaction(session -> {
            validatePositiveId(fromAccountId, "source account id");
            validatePositiveId(toAccountId, "target account id");
            validatePositiveAmount(amount);

            if (fromAccountId == toAccountId) {
                throw new IllegalArgumentException("source and target account id must be different");
            }
            Account accountFrom = findAccountById(session, fromAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId)));
            Account accountTo = findAccountById(session, toAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("No such account: id=%s".formatted(toAccountId)));

            if (amount > accountFrom.getMoneyAmount()) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted transfer=%s"
                                .formatted(accountFrom.getId(), accountFrom.getMoneyAmount(), amount)
                );
            }
            accountFrom.setMoneyAmount(accountFrom.getMoneyAmount() - amount);
            int amountToTransfer = accountTo.getUser().getId() == accountFrom.getUser().getId()
                    ? amount
                    : (int) Math.round(amount * (1 - accountProperties.getTransferCommission()));
            accountTo.setMoneyAmount(accountTo.getMoneyAmount() + amountToTransfer);
        });
    }

    private void validatePositiveId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be > 0");
        }
    }

    private void validatePositiveAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}
