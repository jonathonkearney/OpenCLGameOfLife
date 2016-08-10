package OpenCL;

class Worker extends Thread {

    private GameofLife game;

    public Worker(GameofLife g) {
        game = g;
    }

    public void run() {
        game.work();
    }

}
