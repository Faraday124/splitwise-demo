package model;

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static model.Group.MONEY_ZERO;

@Value
public class User {
    public static final Comparator<User> MAX_DEBT_COMPARATOR = Comparator
            .comparing(u -> Collections.max(u.getDebts()));
    String name;
    List<Debt> debts;

    public User(String name) {
        this.name = name;
        this.debts = new ArrayList<>();
    }

    public User(String name, List<Debt> debts) {
        this.name = name;
        this.debts = debts;
    }

    void addDebt(Debt debt) {
        debts.add(debt);
    }

    Optional<Debt> getDebtByCreditorName(String participant) {
        return debts.stream()
                .filter(d -> d.getAmount().isGreaterThan(MONEY_ZERO))
                .filter(d -> d.getDebtor().equals(participant))
                .findAny();
    }

    @Override
    public String toString() {
        return this.getName() + " \n Borrowed from: " + this.getDebts() + "\n\n";
    }
}
