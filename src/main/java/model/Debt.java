package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.joda.money.Money;

import static model.Group.MONEY_ZERO;

@Data
@AllArgsConstructor
public class Debt implements Comparable<Debt> {
    private Money amount;
    private final String participant;

    public void setZeroAmount() {
        this.amount = MONEY_ZERO;
    }

    @Override
    public String toString() {
        return participant + ", " + amount;
    }

    @Override
    public int compareTo(Debt d) {
        if (this.getAmount().isGreaterThan(d.getAmount())) {
            return 1;
        }
        if (this.getAmount().isLessThan(d.getAmount())) {
            return -1;
        }
        return 0;
    }
}
