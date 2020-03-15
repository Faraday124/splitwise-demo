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
            .comparing(u -> Collections.max(u.getBorrowedFrom()));
    String name;
    List<Debt> borrowedFrom;

    public User(String name) {
        this.name = name;
        this.borrowedFrom = new ArrayList<>();
    }

    public User(String name, List<Debt> borrowedFrom) {
        this.name = name;
        this.borrowedFrom = borrowedFrom;
    }

    public void addBorrowedFrom(Debt debt) {
        borrowedFrom.add(debt);
    }

    public Optional<Debt> getBorrowedFromByName(String participant) {
        return borrowedFrom.stream()
                .filter(d -> d.getAmount().isGreaterThan(MONEY_ZERO))
                .filter(d -> d.getParticipant().equals(participant))
                .findAny();
    }

    @Override
    public String toString() {
        return this.getName() + " \n Borrowed from: " + this.getBorrowedFrom() + "\n\n";
    }
}
