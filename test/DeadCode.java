class DeadCode { 
	public static void main(String[] a) {
        System.out.println(new Doit().doit());
    }
}

class Doit {
    int dead1;
    public int doit() {
        int dead2;
        if (true)
          System.out.println(1);
        else 
          System.out.println(0);
        return 0;
    }
}
