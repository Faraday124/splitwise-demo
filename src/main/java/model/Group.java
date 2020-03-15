package model;

import exception.UserExistsException;
import exception.UserNotFoundException;
import lombok.Value;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static model.User.MAX_DEBT_COMPARATOR;

@Value
public class Group {

    static final Money MONEY_ZERO = Money.zero(CurrencyUnit.USD);
    Map<String, User> userMap = new HashMap<>();

    public void addUser(String userName) throws UserExistsException {
        boolean userExists = userMap.containsKey(userName);

        if (userExists) {
            throw new UserExistsException(format("User %s already exists", userName));
        }
        userMap.put(userName, new User(userName));
    }

    public void addDebt(Money amount, String debtorName, String creditorName) throws UserNotFoundException {
        if (!userMap.containsKey(debtorName) || !userMap.containsKey(creditorName)) {
            throw new UserNotFoundException(format("Cannot add debt for debtor: %s and creditor: %s", debtorName, creditorName));
        }

        User debtor = userMap.get(debtorName);
        User creditor = userMap.get(creditorName);

        handleNewDebt(amount, debtor, creditor);
    }

    private void handleNewDebt(Money amount, User debtor, User creditor) {
        calculateDebts(creditor, debtor, amount);
        transferDebtsForIndirectConnections();
        simplifyDebts();
        removeDebtsWithAmountZero();
    }

    private void removeDebtsWithAmountZero() {
        Predicate<Debt> isGreaterThanZero = debt -> !debt.getAmount().isGreaterThan(MONEY_ZERO);
        getUsers().forEach(u -> u.getBorrowedFrom().removeIf(isGreaterThanZero));
    }

    private void calculateDebts(User creditor, User debtor, Money amount) {
        Optional<Debt> existingDebtorDebt = debtor.getBorrowedFromByName(creditor.getName());

        if (existingDebtorDebt.isPresent()) {
            existingDebtorDebt
                    .ifPresent(d -> d.setAmount(d.getAmount().plus(amount)));
        } else {
            debtor.addBorrowedFrom(new Debt(amount, creditor.getName()));
        }

        calculateDebtsForNewDebt(creditor, debtor);
    }

    private void calculateDebtsForNewDebt(User creditor, User debtor) {
        Optional<Debt> existingDebtorDebt = debtor.getBorrowedFromByName(creditor.getName());
        Optional<Debt> existingCreditorDebt = creditor.getBorrowedFromByName(debtor.getName());

        if (existingDebtorDebt.isPresent() && existingCreditorDebt.isPresent()) {
            Debt borrowed = existingDebtorDebt.get();
            Debt lent = existingCreditorDebt.get();

            if (borrowed.getAmount().isGreaterThan(lent.getAmount())) {
                Money newAmount = borrowed.getAmount().minus(lent.getAmount());
                borrowed.setAmount(newAmount);
                lent.setZeroAmount();
            }
            if (borrowed.getAmount().isLessThan(lent.getAmount())) {
                Money newAmount = lent.getAmount().minus(borrowed.getAmount());
                lent.setAmount(newAmount);
                borrowed.setZeroAmount();
            }
            if (borrowed.getAmount().isEqual(lent.getAmount())) {
                lent.setZeroAmount();
                borrowed.setZeroAmount();
            }
        }
    }

    private User findUserWithTheBiggestDebt() {
        return userMap.values().stream()
                .filter(u -> !u.getBorrowedFrom().isEmpty())
                .max(MAX_DEBT_COMPARATOR)
                .orElse(null);
    }

    private void simplifyDebts() {
        User maxDebtUser = findUserWithTheBiggestDebt();
        if (maxDebtUser == null) {
            return;
        }
        Debt maxDebt = Collections.max(maxDebtUser.getBorrowedFrom());
        Optional<User> maxDebtorOfMaxDebtUser = getUsers().stream()
                .filter(u -> u.getBorrowedFromByName(maxDebtUser.getName()).isPresent())
                .filter(u -> !u.getName().equals(maxDebtUser.getName()))
                .max(MAX_DEBT_COMPARATOR);

        if (!maxDebtorOfMaxDebtUser.isPresent()) {
            return;
        }

        Optional<Debt> creditorForMaxDebt = maxDebtorOfMaxDebtUser.get().getBorrowedFromByName(maxDebtUser.getName());

        if (!creditorForMaxDebt.isPresent()) {
            return;
        }

        transferDebtToSimplify(maxDebtorOfMaxDebtUser.get(), maxDebt, creditorForMaxDebt.get());
    }

    private void transferDebtToSimplify(User maxDebtorOfMaxDebtUser, Debt maxDebt, Debt creditorForMaxDebt) {
        Money maxDebtsDifference = maxDebt.getAmount().minus(creditorForMaxDebt.getAmount());

        maxDebtorOfMaxDebtUser.addBorrowedFrom(new Debt(creditorForMaxDebt.getAmount(), maxDebt.getParticipant()));
        creditorForMaxDebt.setZeroAmount();
        maxDebt.setAmount(maxDebtsDifference);
    }

    private void transferDebtsForIndirectConnections() {

        User maxDebtUser = findUserWithTheBiggestDebt();
        if (maxDebtUser == null) {
            return;
        }
        Debt maxDebt = Collections.max(maxDebtUser.getBorrowedFrom());

        if (maxDebt == null || maxDebt.getAmount().equals(MONEY_ZERO)) {
            return;
        }

        final String creditorForTheBiggestDebt = maxDebt.getParticipant();

        List<Debt> debtsOfCreditor = userMap.get(creditorForTheBiggestDebt).getBorrowedFrom();

        if (debtsOfCreditor.isEmpty()) {
            return;
        }
        simplifyDebts(maxDebtUser, maxDebt, debtsOfCreditor);
    }

    private void simplifyDebts(User maxDebtUser, Debt maxDebt, List<Debt> debtsOfCreditor) {
        Debt maxDebtOfCreditor = Collections.max(debtsOfCreditor);

        Money maxDebtsDifference = maxDebt.getAmount().minus(maxDebtOfCreditor.getAmount());
        if (maxDebtsDifference.isLessThan(MONEY_ZERO)) {
            return;
        }

        transferDebtToOtherUser(maxDebtUser, maxDebt, maxDebtOfCreditor, maxDebtsDifference);
    }

    private void transferDebtToOtherUser(User userTransferTo, Debt debtTransferTo, Debt debtTransferFrom, Money newDebtAmount) {
        userTransferTo.addBorrowedFrom(new Debt(debtTransferFrom.getAmount(), debtTransferFrom.getParticipant()));
        debtTransferFrom.setZeroAmount();
        debtTransferTo.setAmount(newDebtAmount);
    }

    public List<User> getUsers() {
        return new ArrayList<>(userMap.values());
    }

    private String printUsers() {
        return getUsers().stream().map(User::toString).collect(Collectors.joining(""));
    }

    @Override
    public String toString() {
        return "Group Balances: \n" + printUsers();
    }
}
