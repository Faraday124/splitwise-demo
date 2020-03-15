package service;

import exception.UserNotFoundException;
import model.Debt;
import model.Group;
import model.User;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class GroupServiceTest {

    private static final Money MONEY_100_USD = Money.of(CurrencyUnit.USD, new BigDecimal(100));
    private static final Money MONEY_120_USD = Money.of(CurrencyUnit.USD, new BigDecimal(120));
    private static final Money MONEY_140_USD = Money.of(CurrencyUnit.USD, new BigDecimal(140));
    private static final Money MONEY_1000_USD = Money.of(CurrencyUnit.USD, new BigDecimal(1000));

    private static final String USER_JOHN = "John";
    private static final String USER_BEN = "Ben";
    private static final String USER_MIKE = "Mike";

    private BalanceService balanceService;

    @Test
    public void shouldReturnCorrectAmount() throws Exception {
        User iwo = new User("Iwo");
        User adam = new User("Adam");
        User john = new User("John");

        Group homeGroup = new Group();
        homeGroup.addUser(iwo);
        homeGroup.addUser(adam);
        homeGroup.addUser(john);

        homeGroup.addDebt(MONEY_100_USD, iwo, adam);
        homeGroup.addDebt(MONEY_120_USD, adam, iwo);
        homeGroup.addDebt(MONEY_120_USD, adam, iwo);
        homeGroup.addDebt(MONEY_140_USD, iwo, adam);
        homeGroup.addDebt(MONEY_140_USD, iwo, john);
        homeGroup.addDebt(MONEY_100_USD, john, adam);
        homeGroup.addDebt(MONEY_120_USD, adam, iwo);

        System.out.println(homeGroup);
    }

    @Test
    public void shouldReturnCorrectAmountForTwoPeople() throws Exception {
        //given
        Group homeGroup = new Group();

        User ben = new User(USER_BEN);
        User john = new User(USER_JOHN);

        Money expectedMoney = Money.of(CurrencyUnit.USD, 200);
        List<Debt> expectedDebts = Arrays.asList(new Debt(expectedMoney, USER_BEN));
        List<Debt> expectedLoans = Arrays.asList(new Debt(expectedMoney, USER_JOHN));

        User expectedBen = new User(USER_BEN, emptyList(), expectedLoans);
        User expectedJohn = new User(USER_JOHN, expectedDebts, emptyList());
        List<User> expected = Arrays.asList(expectedJohn, expectedBen);

        //when
        homeGroup.addUser(ben);
        homeGroup.addUser(john);

        homeGroup.addDebt(MONEY_1000_USD, ben, john);
        homeGroup.addDebt(Money.of(CurrencyUnit.USD, 1200), john, ben);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSimplifyDebts() throws Exception {
        //given
        Group homeGroup = new Group();

        User ben = new User(USER_BEN);
        User john = new User(USER_JOHN);
        User mike = new User(USER_MIKE);

        List<Debt> expectedDebts = Arrays.asList(new Debt(MONEY_1000_USD, USER_MIKE));
        List<Debt> expectedLoans = Arrays.asList(new Debt(MONEY_1000_USD, USER_JOHN));

        User expectedBen = new User(USER_BEN, emptyList(), emptyList());
        User expectedJohn = new User(USER_JOHN, expectedDebts, emptyList());
        User expectedMike = new User(USER_MIKE, emptyList(), expectedLoans);
        List<User> expected = Arrays.asList(expectedMike, expectedJohn, expectedBen);

        //when
        homeGroup.addUser(ben);
        homeGroup.addUser(john);
        homeGroup.addUser(mike);

        homeGroup.addDebt(MONEY_1000_USD, john, ben);
        homeGroup.addDebt(MONEY_1000_USD, ben, mike);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSimplifyDebtsForManyPeople() throws Exception {
        //given
        Group homeGroup = new Group();

        User ben = new User(USER_BEN);
        User john = new User(USER_JOHN);
        User mike = new User(USER_MIKE);

        List<Debt> expectedDebts = Arrays.asList(new Debt(MONEY_1000_USD, USER_MIKE));
        List<Debt> expectedLoans = Arrays.asList(new Debt(MONEY_1000_USD, USER_JOHN));

        User expectedBen = new User(USER_BEN, emptyList(), emptyList());
        User expectedJohn = new User(USER_JOHN, expectedDebts, emptyList());
        User expectedMike = new User(USER_MIKE, emptyList(), expectedLoans);
        User expectedFrank = new User("Frank", emptyList(), emptyList());
        User expectedGreg = new User("Greg", emptyList(), emptyList());

        List<User> expected = Arrays.asList(
                expectedMike, expectedJohn, expectedBen, expectedFrank, expectedGreg);

        //when
        homeGroup.addUser(ben);
        homeGroup.addUser(john);
        homeGroup.addUser(mike);
        homeGroup.addUser(new User("Frank"));
        homeGroup.addUser(new User("Greg"));

        homeGroup.addDebt(MONEY_100_USD, ben, john); //ben john 100
        homeGroup.addDebt(MONEY_120_USD, john, ben); //john ben 20
        homeGroup.addDebt(MONEY_120_USD, mike, ben); //mike ben 120, john ben 20
        homeGroup.addDebt(MONEY_140_USD, ben, john); //mike ben 120, ben john 120 -> mike john 120
        homeGroup.addDebt(Money.of(CurrencyUnit.USD, 200), john, ben);
        //mike john 120, john ben 200 ->
        //mike ben 120, john ben 80

        //mike john 120, john ben 200 ->
        //mike ben 120, john ben 80
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnNoDebts() throws Exception {
        //given
        Group homeGroup = new Group();

        User ben = new User(USER_BEN);
        User john = new User(USER_JOHN);

        List<User> expected = Arrays.asList(
                new User(USER_JOHN, emptyList(), emptyList()),
                new User(USER_BEN, emptyList(), emptyList()));
        //when
        homeGroup.addUser(ben);
        homeGroup.addUser(john);

        homeGroup.addDebt(MONEY_1000_USD, john, ben);
        homeGroup.addDebt(MONEY_1000_USD, ben, john);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowErrorWhenNoUserDoesNotExist() throws Exception {
        //given
        Group homeGroup = new Group();

        User ben = new User(USER_BEN);
        User john = new User(USER_JOHN);
        homeGroup.addUser(ben);

        Assert.assertThrows(UserNotFoundException.class, () -> homeGroup.addDebt(MONEY_1000_USD, john, ben));
    }
}
