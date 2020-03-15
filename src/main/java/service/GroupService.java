package service;

import exception.UserExistsException;
import exception.UserNotFoundException;
import model.User;
import org.joda.money.Money;

import java.util.List;

public interface GroupService {

    void addDebt(Money amount, String debtorName, String creditorName) throws UserNotFoundException;

    void addUserToGroup(String userName) throws UserExistsException;

    List<User> getUsers();
}
