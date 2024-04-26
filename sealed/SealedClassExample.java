
//javac SealedClassExample.java
//java SealedClassExample
public class SealedClassExample
{
    public static void main(String args[])
    {
        Person grandfather = new GrandFather(87, "Albert");
        grandfather.name = "Albert";
        System.out.println("The age of Person is: "+getAge(grandfather));
        Person person = null;
        System.out.println("The age of Person is: "+getAge(person));
    }
    public static int getAge(Person person)
    {
        if (person instanceof Father)
        {
            return ((Father) person).getFatherAge();
        } else if (person instanceof GrandFather)
        {
            return ((GrandFather) person).getGrandFatherAge();
        }
        return new GrandSon(10).getGrandSonAge();
    }
}
//the class person extends only Father and GrandFather class
abstract sealed class Person permits Father, GrandFather
{
    String name;
    String getName()
    {
        return name;
    }
}

final class Father extends Person
{
    String name;
    int age;
    //constructor of the Father class
    Father(int age, String name)
    {
        this.age = age;
        this.name = name;
    }
    int getFatherAge()
    {
        return age;
    }
}
//non-sealed class extends unknown subclass (Person)
non-sealed class GrandFather extends Person
{
    int age;
    GrandFather(int age, String name)
    {
        this.age = age;
        this.name = name;
    }
    int getGrandFatherAge()
    {
        return age;
    }
}

class GrandSon {
    int age;
    GrandSon(int age)
    {
        this.age = age;
    }

    int getGrandSonAge()
    {
        return age;
    }
}