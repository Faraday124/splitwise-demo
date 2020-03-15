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
    private final Map<String, User> userMap = new HashMap<>();

    public void addUser(User user) throws UserExistsException {
        boolean userExists = userMap.containsKey(user.getName());

        if (userExists) {
            throw new UserExistsException(format("User %s already exists", user.getName()));
        }
        userMap.put(user.getName(), user);
    }

    public void addDebt(Money amount, User debtor, User creditor) throws UserNotFoundException {
        if (!userMap.containsKey(debtor.getName()) || !userMap.containsKey(creditor.getName())) {
            throw new UserNotFoundException(format("Cannot add debt for debtor: %s and creditor: %s", debtor.getName(), creditor.getName()));
        }
        Debt debt = new Debt(amount, debtor.getName());

        handleBorrowingMoney(creditor, userMap.get(debtor.getName()), debt);
        handleLendingMoney(creditor, userMap.get(creditor.getName()), debt);
        calculateDebtsForTwoUsers(debtor, creditor);
        simplifyAllDebts();
        removeDebtsWithAmountZero();
    }

    private void removeDebtsWithAmountZero() {
        Predicate<Debt> isGreaterThanZero = debt -> !debt.getAmount().isGreaterThan(MONEY_ZERO);
        userMap.values()
                .forEach(u -> u.getBorrowedFrom().removeIf(isGreaterThanZero));
        userMap.values()
                .forEach(u -> u.getLentTo().removeIf(isGreaterThanZero));
    }

    private void handleLendingMoney(User creditor, User debtor, Debt newDebt) {
        Optional<Debt> existingDebt = creditor.getLentTo().stream()
                .filter(d -> d.getParticipant().equals(debtor.getName()))
                .findAny();

        if (existingDebt.isPresent()) {
            existingDebt
                    .ifPresent(d -> d.setAmount(d.getAmount().plus(newDebt.getAmount())));
        } else {
            creditor.addLentTo(newDebt);
        }
    }

    private void handleBorrowingMoney(User creditor, User debtor, Debt newDebt) {
        Optional<Debt> existingDebt = debtor.getBorrowedFrom().stream()
                .filter(d -> d.getParticipant().equals(creditor.getName()))
                .findAny();

        if (existingDebt.isPresent()) {
            existingDebt
                    .ifPresent(d -> d.setAmount(d.getAmount().plus(newDebt.getAmount())));
        } else {
            debtor.addBorrowedFrom(new Debt(newDebt.getAmount(), creditor.getName()));
        }
    }

    private void calculateDebtForUser(User user) {
        for (Debt borrowed : user.getBorrowedFrom()) {
            for (Debt lent : user.getLentTo()) {
                if (borrowed.getParticipant().equals(lent.getParticipant())) {
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
        }
    }

    private void calculateDebtsForTwoUsers(User debtor, User creditor) {
        calculateDebtForUser(debtor);
        calculateDebtForUser(creditor);
    }

    private User findUserWithTheBiggestDebt() {
        return userMap.values().stream()
                .filter(u -> !u.getBorrowedFrom().isEmpty())
                .max(MAX_DEBT_COMPARATOR).get();
    }

    private void simplifyAllDebts() {

        User maxDebtUser = findUserWithTheBiggestDebt();
        Debt maxDebt = Collections.max(maxDebtUser.getBorrowedFrom());

        if (maxDebt == null) {
            return;
        }

        final String creditorForTheBiggestDebt = maxDebt.getParticipant();
        final String debtorForTheBiggestDebt = maxDebtUser.getName();

        List<Debt> debtsOfCreditor = userMap.get(creditorForTheBiggestDebt).getBorrowedFrom();

        if (debtsOfCreditor.isEmpty()) {
            return;
        }
        simplifyDebts(maxDebtUser, maxDebt, creditorForTheBiggestDebt, debtorForTheBiggestDebt, debtsOfCreditor);
    }

    private void simplifyDebts(User maxDebtUser, Debt maxDebt, String creditorForTheBiggestDebt, String debtorForTheBiggestDebt, List<Debt> debtsOfCreditor) {
        Debt maxDebtOfCreditor = Collections.max(debtsOfCreditor);

        Money maxDebtsDifference = maxDebt.getAmount().minus(maxDebtOfCreditor.getAmount());
        User simplifiedDebtUser = userMap.get(maxDebtOfCreditor.getParticipant());
        //iwo -> adam
        maxDebtUser.addBorrowedFrom(new Debt(maxDebtOfCreditor.getAmount(), maxDebtOfCreditor.getParticipant()));
        //adam -> john
        simplifiedDebtUser
                .getLentToByName(userMap.get(creditorForTheBiggestDebt).getName())
                .ifPresent(Debt::setZeroAmount);
        //adam -> iwo
        simplifiedDebtUser
                .addLentTo(new Debt(maxDebtOfCreditor.getAmount(), debtorForTheBiggestDebt));

        //john -> adam
        maxDebtOfCreditor.setZeroAmount();
        //iwo -> john
        maxDebt.setAmount(maxDebtsDifference);
        //john -> iwo
        userMap.get(creditorForTheBiggestDebt).getLentToByName(debtorForTheBiggestDebt)
                .ifPresent(debt -> debt.setAmount(maxDebtsDifference));
    }

    public List<User> getUsers() {
        return new ArrayList<>(userMap.values());
    }

    @Override
    public String toString() {
        return "Grop Balances: \n" + userMap.values().stream().map(User::toString).collect(Collectors.joining(""));
    }

    //iwo -> adam, 100 usd
    //adam -> iwo, 120 usd          iwo receives 20, adam owes 20

    //marian -> adam, 40 usd

    //marian -> iwo 20, usd
    //marian -> adam 20, usd
}
