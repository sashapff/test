package ru.ifmo.rain.ivanova.bank;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class BankTest {
    private static final int PORT = 4040;
    private static Bank bank;
    private static Registry registry;
    private static final int globalPassport = 0;
    private static final String globalFirstName = "First";
    private static final String globalLastName = "Last";
    private static final String globalAccountId = "Account";
    private static final int globalAddition = 50;
    private static final int size = 10;

    @BeforeClass
    public static void beforeClass() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
    }

    @Before
    public void before() throws RemoteException {
        bank = new RemoteBank(PORT);
        final Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, 0);
        registry.rebind("bank", stub);
        System.out.println("New test!");
    }

    private void checkPerson(final Person person) throws RemoteException {
        checkPerson(person, globalPassport, globalFirstName, globalLastName);
    }

    private void checkPerson(final Person person, final int passport,
                             final String firstName, final String lastName) throws RemoteException {
        assertNotNull(person);
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
        assertEquals(passport, person.getPassport());
    }

    @Test
    public void test00_createPerson() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person);
    }

    private Person getRemotePerson() throws RemoteException {
        return getRemotePerson(globalPassport, globalFirstName, globalLastName);
    }

    private Person getRemotePerson(final int passport, final String firstName,
                                   final String lastName) throws RemoteException {
        final Person person = bank.createPerson(passport, firstName, lastName);
        checkPerson(person, passport, firstName, lastName);
        final Person remotePerson = bank.getRemotePerson(passport);
        checkPerson(remotePerson, passport, firstName, lastName);
        return remotePerson;
    }

    private Person getLocalPerson() throws RemoteException {
        return getLocalPerson(globalPassport, globalFirstName, globalLastName);
    }

    private Person getLocalPerson(final int passport, final String firstName,
                                  final String lastName) throws RemoteException {
        final Person person = bank.createPerson(passport, firstName, lastName);
        checkPerson(person, passport, firstName, lastName);
        final Person localPerson = bank.getLocalPerson(passport);
        checkPerson(localPerson, passport, firstName, lastName);
        return localPerson;
    }

    @Test
    public void test01_getRemotePerson() throws RemoteException {
        getRemotePerson();
    }

    @Test
    public void test02_getLocalPerson() throws RemoteException {
        getLocalPerson();
    }

    @Test
    public void test03_createPersonTwice() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person, globalPassport, globalFirstName, globalLastName);
        assertEquals(person, bank.createPerson(globalPassport, globalFirstName, globalLastName));
    }

    @Test
    public void test04_getNotExistingPerson() throws RemoteException {
        assertNull(bank.getRemotePerson(globalPassport));
        assertNull(bank.getRemotePerson(globalPassport));
    }

    private String toString(final int i) {
        return Integer.toString(i);
    }

    @Test
    public void test05_getManyPersons() throws RemoteException {
        for (int i = 0; i < size; i++) {
            final int passport = i;
            final String firstName = toString(i);
            final String lastName = toString(i + size);
            getRemotePerson(passport, firstName, lastName);
            getLocalPerson(passport, firstName, lastName);
        }
    }

    private String getFullAccountId(final String id, final int passport) {
        return passport + ":" + id;
    }

    private void checkAccount(final Account account, final Person person) throws RemoteException {
        checkAccount(account, person, globalAccountId, globalPassport, 0);
    }

    private void checkAccount(final Account account, final Person person,
                              final String accountId, final int passport, final int amount) throws RemoteException {
        assertNotNull(account);
        assertEquals(account, bank.getAccount(accountId, person));
        assertEquals(amount, account.getAmount());
        if (person instanceof LocalPerson) {
            assertEquals(accountId, account.getId());
        } else {
            assertEquals(getFullAccountId(accountId, passport), account.getId());
        }
    }

    @Test
    public void test06_createAccount() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person);
        final Account account = bank.createAccount(globalAccountId, person);
        checkAccount(account, person);
    }

    @Test
    public void test07_getRemotePersonAccount() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        final Account account = bank.createAccount(globalAccountId, remotePerson);
        checkAccount(account, remotePerson);
    }

    @Test
    public void test08_getLocalPersonAccount() throws RemoteException {
        final Person localPerson = getLocalPerson();
        final Account account = bank.createAccount(globalAccountId, localPerson);
        checkAccount(account, localPerson);
    }

    @Test
    public void test09_createAccountTwice() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person, globalPassport, globalFirstName, globalLastName);
        final Account account = bank.createAccount(globalAccountId, person);
        checkAccount(account, person, globalAccountId, globalPassport, 0);
        assertEquals(account, bank.createAccount(globalAccountId, person));
    }

    @Test
    public void test10_createRemotePersonAccountTwice() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        final Account account = bank.createAccount(globalAccountId, remotePerson);
        checkAccount(account, remotePerson);
        assertEquals(account, bank.createAccount(globalAccountId, remotePerson));
    }

    @Test
    public void test11_createLocalPersonAccountTwice() throws RemoteException {
        final Person localPerson = getLocalPerson();
        final Account account = bank.createAccount(globalAccountId, localPerson);
        checkAccount(account, localPerson);
        assertEquals(account, bank.createAccount(globalAccountId, localPerson));
    }

    @Test
    public void test12_getNotExistingAccount() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person);
        final String accountId = toString(100500);
        assertNull(bank.getAccount(accountId, person));
        final Person remotePerson = getRemotePerson();
        assertNull(bank.getAccount(accountId, remotePerson));
        final Person localPerson = getLocalPerson();
        assertNull(bank.getAccount(accountId, localPerson));
    }

    private void addAccounts(Person localPerson) throws RemoteException {
        for (int i = 0; i < size; i++) {
            assertEquals(i, bank.getAccounts(localPerson).size());
            final String accountId = toString(i);
            final Account account = bank.createAccount(accountId, localPerson);
            checkAccount(account, localPerson, accountId, localPerson.getPassport(), 0);
        }
    }

    @Test
    public void test13_createManyPersonAccounts() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        checkPerson(person);
        addAccounts(person);
    }

    @Test
    public void test14_addRemotePersonAccount() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        assertEquals(0, bank.getAccounts(remotePerson).size());
        final Account account = bank.createAccount(globalAccountId, remotePerson);
        checkAccount(account, remotePerson);
        assertEquals(1, bank.getAccounts(remotePerson).size());
    }

    @Test
    public void test15_addLocalPersonAccount() throws RemoteException {
        final Person localPerson = getLocalPerson();
        assertEquals(0, bank.getAccounts(localPerson).size());
        final Account account = bank.createAccount(globalAccountId, localPerson);
        checkAccount(account, localPerson);
        assertEquals(1, bank.getAccounts(localPerson).size());
    }

    @Test
    public void test16_addManyAccountsToRemotePerson() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        addAccounts(remotePerson);
    }

    @Test
    public void test17_addManyAccountsToLocalPerson() throws RemoteException {
        final Person localPerson = getLocalPerson();
        addAccounts(localPerson);
    }

    @Test
    public void test18_addManyAccountsToManyRemotePersons() throws RemoteException {
        for (int j = 0; j < size; j++) {
            final int passport = j;
            final String firstName = toString(j);
            final String lastName = toString(j + size);
            final Person remotePerson = getRemotePerson(passport, firstName, lastName);
            addAccounts(remotePerson);
        }
    }

    @Test
    public void test19_addManyAccountsToManyLocalPersons() throws RemoteException {
        for (int j = 0; j < size; j++) {
            final int passport = j;
            final String firstName = toString(j);
            final String lastName = toString(j + size);
            final Person localPerson = getLocalPerson(passport, firstName, lastName);
            addAccounts(localPerson);
        }
    }

    @Test
    public void test20_getLocalPersonAccountsAddingBeforeCreating() throws RemoteException {
        for (int j = 0; j < size; j++) {
            final int passport = j;
            final String firstName = toString(j);
            final String lastName = toString(j + size);
            final Person remotePerson = getRemotePerson(passport, firstName, lastName);
            addAccounts(remotePerson);
            final Person localPerson = getLocalPerson(passport, firstName, lastName);
            assertEquals(size, bank.getAccounts(localPerson).size());
        }
    }

    @Test
    public void test21_getLocalPersonAccountsAddingAfterCreating() throws RemoteException {
        for (int j = 0; j < size; j++) {
            final int passport = j;
            final String firstName = toString(j);
            final String lastName = toString(j + size);
            final Person remotePerson = getRemotePerson(passport, firstName, lastName);
            final Person localPerson = getLocalPerson(passport, firstName, lastName);
            addAccounts(remotePerson);
            assertEquals(0, bank.getAccounts(localPerson).size());
        }
    }

    @Test
    public void test22_setAmountInLocalAccountCreatingLocalAccountAfterLocalPerson() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        final Person localPerson = getLocalPerson();
        final Account localAccount = bank.createAccount(globalAccountId, localPerson);
        assertNotNull(localAccount);
        final Account remoteAccount = bank.getAccount(globalAccountId, remotePerson);
        assertNotNull(remoteAccount);
        localAccount.setAmount(globalAddition);
        assertEquals(globalAddition, localAccount.getAmount());
        assertEquals(0, remoteAccount.getAmount());
    }

    @Test
    public void test23_setAmountInRemoteAccountCreatingRemoteAccountAfterLocalPerson() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        final Person localPerson = getLocalPerson();
        final Account remoteAccount = bank.createAccount(globalAccountId, remotePerson);
        assertNotNull(remoteAccount);
        final Account localAccount = bank.getAccount(globalAccountId, localPerson);
        assertNotNull(localAccount);
        assertEquals(localAccount, remoteAccount);
        remoteAccount.setAmount(globalAddition);
        assertEquals(globalAddition, remoteAccount.getAmount());
        assertEquals(globalAddition, localAccount.getAmount());
    }

    @Test
    public void test24_setAmountInRemoteAccountCreatingRemoteAccountBeforeLocalPerson() throws RemoteException {
        final Person remotePerson = getRemotePerson();
        final Account remoteAccount = bank.createAccount(globalAccountId, remotePerson);
        assertNotNull(remoteAccount);
        Person localPerson = getLocalPerson();
        Account localAccount = bank.getAccount(globalAccountId, localPerson);
        assertNotNull(localAccount);
        remoteAccount.setAmount(globalAddition);
        assertEquals(getFullAccountId(globalAccountId, remotePerson.getPassport()), remoteAccount.getId());
        assertEquals(globalAddition, remoteAccount.getAmount());
        localAccount = bank.getAccount(globalAccountId, localPerson);
        assertEquals(0, localAccount.getAmount());
        localPerson = bank.getLocalPerson(globalPassport);
        localAccount = bank.getAccount(globalAccountId, localPerson);
        assertEquals(globalAddition, localAccount.getAmount());
    }

    @Test
    public void test25_setAmountOfAccount() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        assertNotNull(person);
        final Person localPerson = bank.getLocalPerson(globalPassport);
        assertNotNull(localPerson);
        Account localAccount = bank.createAccount(globalAccountId, localPerson);
        localAccount.setAmount(globalAddition);
        localAccount = bank.getAccount(globalAccountId, localPerson);
        assertEquals(localAccount.getAmount(), globalAddition);
        final Person remotePerson = bank.getRemotePerson(globalPassport);
        final Account remoteAccount = bank.getAccount(globalAccountId, remotePerson);
        assertEquals(remoteAccount.getAmount(), 0);
    }

    @Test
    public void test26_manyAccountsOfOneRemotePerson() throws RemoteException {
        final Person person = bank.createPerson(globalPassport, globalFirstName, globalLastName);
        assertNotNull(person);
        final List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            persons.add(bank.getRemotePerson(globalPassport));
        }
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                assertNotNull(bank.createAccount(i + "+" + j, persons.get(j)));
            }
        }
        for (int i = 0; i < 100; i++) {
            assertEquals(10000, bank.getAccounts(persons.get(i)).size());
        }
    }

    @Test
    public void test27_setAmountOfManyAccountsOfManyPersons() throws RemoteException {
        final List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final Person person = bank.createPerson(i, globalFirstName, globalLastName);
            assertNotNull(person);
            persons.add(bank.getRemotePerson(i));
        }
        final Map<String, Integer> answer = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                final String accountId = i + "+" + j;
                final Account account = bank.createAccount(accountId, persons.get(j));
                assertNotNull(account);
                account.setAmount(i + j);
                answer.put(accountId, i + j);
            }
        }
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                final String accountId = i + "+" + j;
                assertEquals((int) answer.get(accountId), bank.getAccount(accountId, persons.get(j)).getAmount());
            }
        }
    }

    @Test
    public void test28_setAmountTwice() throws RemoteException {
        for (var i = 0; i < 100; ++i) {
            final Person person = bank.createPerson(globalPassport + i,
                    globalFirstName + i, globalLastName + i);
            assertNotNull(person);
        }

        for (var i = 0; i < 100; i++) {
            final Person person = bank.getRemotePerson(globalPassport + i);
            for (var j = 0; j < 100; j++) {
                bank.createAccount("Account" + j, person);
            }
        }

        for (var i = 0; i < 100; i++) {
            final Person person = bank.getRemotePerson(globalPassport + i);
            for (var j = 0; j < 100; j++) {
                final Account account = bank.getAccount("Account" + j, person);
                assertNotNull(account);
                account.setAmount(account.getAmount() + 50);
                assertEquals(50, account.getAmount());
            }
        }

        for (var i = 0; i < 100; i++) {
            final Person person = bank.getRemotePerson(globalPassport + i);
            for (var j = 0; j < 100; j++) {
                final Account account = bank.getAccount("Account" + j, person);
                assertNotNull(account);
                account.setAmount(account.getAmount() - 50);
                assertEquals(0, account.getAmount());
            }
        }
    }

    @Test
    public void test29_client() throws RemoteException {
        Client.main(globalFirstName, globalLastName,
                toString(globalPassport), globalAccountId, toString(globalAddition));
        final Person person = bank.getRemotePerson(globalPassport);
        assertNotNull(person);
        assertEquals(globalPassport, person.getPassport());
        assertEquals(globalFirstName, person.getFirstName());
        assertEquals(globalLastName, person.getLastName());
        final Account account = bank.getAccount(globalAccountId, person);
        assertNotNull(account);
        assertEquals(getFullAccountId(globalAccountId, person.getPassport()), account.getId());
        assertEquals(globalAddition, account.getAmount());

        Client.main(globalFirstName + "incorrect", globalLastName + "incorrect",
                toString(globalPassport), globalAccountId, toString(globalAddition));
        assertEquals(globalPassport, person.getPassport());
        assertEquals(globalFirstName, person.getFirstName());
        assertEquals(globalLastName, person.getLastName());
        assertEquals(getFullAccountId(globalAccountId, person.getPassport()), account.getId());
        assertEquals(globalAddition, account.getAmount());

        Client.main(globalFirstName, globalLastName,
                toString(globalPassport), globalAccountId, toString(globalAddition));
        assertEquals(globalPassport, person.getPassport());
        assertEquals(globalFirstName, person.getFirstName());
        assertEquals(globalLastName, person.getLastName());
        assertEquals(getFullAccountId(globalAccountId, person.getPassport()), account.getId());
        assertEquals(2 * globalAddition, account.getAmount());

        Client.main(globalFirstName, globalLastName,
                toString(globalPassport), globalAccountId);
        assertEquals(globalPassport, person.getPassport());
        assertEquals(globalFirstName, person.getFirstName());
        assertEquals(globalLastName, person.getLastName());
        assertEquals(getFullAccountId(globalAccountId, person.getPassport()), account.getId());
        assertEquals(2 * globalAddition, account.getAmount());
    }

}
