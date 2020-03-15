package service;

import lombok.Value;
import model.Group;
import org.joda.money.Money;

@Value
public class DebtService {

    private final Group group;

    public void addDebtToAGroup(Group group, Money amount) {
    }
}
