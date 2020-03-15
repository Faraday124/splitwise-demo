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
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class GroupServiceTest {

    private static final Money MONEY_100_USD = Money.of(CurrencyUnit.USD, new BigDecimal(100));
    private static final Money MONEY_120_USD = Money.of(CurrencyUnit.USD, new BigDecimal(120));
    private static final Money MONEY_140_USD = Money.of(CurrencyUnit.USD, new BigDecimal(140));
    private static final Money MONEY_1000_USD = Money.of(CurrencyUnit.USD, new BigDecimal(1000));

    private static final String USER_JOHN = "John";
    private static final String USER_BEN = "Ben";
    private static final String USER_MIKE = "Mike";

    @Test
    public void shouldReturnCorrectAmountForTwoPeople() throws Exception {
        //given
        Group homeGroup = new Group();

        Money expectedMoney = Money.of(CurrencyUnit.USD, 200);
        List<Debt> expectedDebts = singletonList(new Debt(expectedMoney, USER_BEN));

        User expectedBen = new User(USER_BEN, emptyList());
        User expectedJohn = new User(USER_JOHN, expectedDebts);
        List<User> expected = asList(expectedJohn, expectedBen);

        //when
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_JOHN);

        homeGroup.addDebt(MONEY_1000_USD, USER_BEN, USER_JOHN);
        homeGroup.addDebt(Money.of(CurrencyUnit.USD, 1200), USER_JOHN, USER_BEN);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSimplifyDebts() throws Exception {
        //given
        Group homeGroup = new Group();

        List<Debt> expectedDebts = singletonList(new Debt(MONEY_1000_USD, USER_MIKE));

        User expectedBen = new User(USER_BEN, emptyList());
        User expectedJohn = new User(USER_JOHN, expectedDebts);
        User expectedMike = new User(USER_MIKE, emptyList());
        List<User> expected = asList(expectedMike, expectedJohn, expectedBen);

        //when
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_JOHN);
        homeGroup.addUser(USER_MIKE);

        homeGroup.addDebt(MONEY_1000_USD, USER_JOHN, USER_BEN);
        homeGroup.addDebt(MONEY_1000_USD, USER_BEN, USER_MIKE);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSimplifyWithManyConnections() throws Exception {
        //given
        Group homeGroup = new Group();
        String greg = "Greg";
        String frank = "Frank";

        List<Debt> mikeExpectedDebts = singletonList(new Debt(MONEY_120_USD, frank));

        User expectedBen = new User(USER_BEN, emptyList());
        User expectedJohn = new User(USER_JOHN, emptyList());
        User expectedMike = new User(USER_MIKE, mikeExpectedDebts);
        User expectedGreg = new User(greg, emptyList());
        User expectedFrank = new User(frank, emptyList());

        List<User> expected = asList(
                expectedMike, expectedJohn, expectedBen, expectedGreg, expectedFrank);

        //when
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_JOHN);
        homeGroup.addUser(USER_MIKE);
        homeGroup.addUser(greg);
        homeGroup.addUser(frank);

        homeGroup.addDebt(MONEY_120_USD, USER_MIKE, USER_JOHN); //mike john 120
        homeGroup.addDebt(MONEY_120_USD, USER_JOHN, USER_BEN); //mike ben 120
        homeGroup.addDebt(MONEY_120_USD, USER_BEN, greg); // mike greg 120
        homeGroup.addDebt(MONEY_120_USD, greg, frank); // mike frank 120

        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSimplifyDebtsForManyPeople() throws Exception {
        //given
        Group homeGroup = new Group();

        List<Debt> mikeExpectedDebts = singletonList(new Debt(MONEY_120_USD, USER_BEN));
        List<Debt> johnExpectedDebts = singletonList(new Debt(Money.of(CurrencyUnit.USD, 80), USER_BEN));

        User expectedBen = new User(USER_BEN, emptyList());
        User expectedJohn = new User(USER_JOHN, johnExpectedDebts);
        User expectedMike = new User(USER_MIKE, mikeExpectedDebts);

        List<User> expected = asList(
                expectedMike, expectedJohn, expectedBen);

        //when
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_JOHN);
        homeGroup.addUser(USER_MIKE);

        homeGroup.addDebt(MONEY_100_USD, USER_BEN, USER_JOHN); //ben john 100
        homeGroup.addDebt(MONEY_120_USD, USER_JOHN, USER_BEN); //john ben 20
        homeGroup.addDebt(MONEY_120_USD, USER_MIKE, USER_BEN); //mike ben 120, john ben 20
        homeGroup.addDebt(MONEY_140_USD, USER_BEN, USER_JOHN); //mike ben 120, ben john 120 -> mike john 120
        homeGroup.addDebt(Money.of(CurrencyUnit.USD, 200), USER_JOHN, USER_BEN);
        //mike john 120, john ben 200 -> mike ben 120, john ben 80

        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnNoDebts() throws Exception {
        //given
        Group homeGroup = new Group();

        List<User> expected = asList(
                new User(USER_JOHN, emptyList()),
                new User(USER_BEN, emptyList()));
        //when
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_JOHN);

        homeGroup.addDebt(MONEY_1000_USD, USER_JOHN, USER_BEN);
        homeGroup.addDebt(MONEY_1000_USD, USER_BEN, USER_JOHN);
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowErrorWhenNoUserDoesNotExist() throws Exception {
        //given
        Group homeGroup = new Group();

        homeGroup.addUser(USER_BEN);

        Assert.assertThrows(UserNotFoundException.class, () -> homeGroup.addDebt(MONEY_1000_USD, USER_JOHN, USER_BEN));
    }

    @Test
    public void shouldReturnCorrectAmount() throws Exception {
        //given

        Group homeGroup = new Group();
        homeGroup.addUser(USER_BEN);
        homeGroup.addUser(USER_MIKE);
        homeGroup.addUser(USER_JOHN);

        Money expectedMoney = Money.of(CurrencyUnit.USD, 20);
        List<Debt> expectedDebts = singletonList(new Debt(expectedMoney, USER_JOHN));
        User expectedBen = new User(USER_BEN, expectedDebts);
        User expectedMike = new User(USER_MIKE, emptyList());
        User expectedJohn = new User(USER_JOHN, emptyList());
        List<User> expected = asList(expectedMike, expectedJohn, expectedBen);

        //when
        homeGroup.addDebt(MONEY_100_USD, USER_BEN, USER_MIKE); //ben mike 100,
        homeGroup.addDebt(MONEY_120_USD, USER_MIKE, USER_BEN); //mike ben 20,
        homeGroup.addDebt(MONEY_120_USD, USER_MIKE, USER_BEN); //mike ben 140,
        homeGroup.addDebt(MONEY_140_USD, USER_BEN, USER_MIKE); // []
        homeGroup.addDebt(MONEY_140_USD, USER_BEN, USER_JOHN); //ben john 140,
        homeGroup.addDebt(MONEY_120_USD, USER_JOHN, USER_MIKE); //ben mike 100, ben john 20,
        homeGroup.addDebt(MONEY_120_USD, USER_MIKE, USER_BEN); // ben john 20
        List<User> actual = homeGroup.getUsers();

        //then
        assertThat(actual).isEqualTo(expected);
    }
}
