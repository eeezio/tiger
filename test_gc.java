class test {
    public static void main(String[] a) {
        System.out.println(new fuck().Start(5));
    }
}

class aaa {
    int a;
    int b;
    int c;
}

class fuck {
    public int Start(int num) {
        int i;
        aaa j;

        i = 0;
        while (i < num) {
            j = new aaa();
            i = i + 1;
        }
        return 1;
    }


}