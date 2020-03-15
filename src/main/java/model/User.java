package model;

import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Value
public class User {
    public static final Comparator<User> MAX_DEBT_COMPARATOR = Comparator
            .comparing(u -> Collections.max(u.getBorrowedFrom()));
    private final String name;
    @Singular
    @EqualsAndHashCode.Exclude
    private final List<Debt> borrowedFrom;
    @Singular
    @EqualsAndHashCode.Exclude
    private final List<Debt> lentTo;

    public User(String name) {
        this.name = name;
        this.borrowedFrom = new ArrayList<>();
        this.lentTo = new ArrayList<>();
    }

    public User(String name, List<Debt> borrowedFrom, List<Debt> lentTo) {
        this.name = name;
        this.borrowedFrom = borrowedFrom;
        this.lentTo = lentTo;
    }

    public void addLentTo(Debt debt) {
        lentTo.add(debt);
    }

    public void addBorrowedFrom(Debt debt) {
        borrowedFrom.add(debt);
    }

    public Optional<Debt> getLentToByName(String participant) {
        return lentTo.stream()
                .filter(d -> d.getParticipant().equals(participant))
                .findAny();
    }

    @Override
    public String toString() {
        return this.getName() + " \n Borrowed from: " + this.getBorrowedFrom()
                + " \n Lent to: " + this.getLentTo() + "\n\n";
    }
}
