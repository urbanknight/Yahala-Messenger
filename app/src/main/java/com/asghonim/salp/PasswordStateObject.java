package com.asghonim.salp;


class PasswordStateObject {
    private final StringBuilder builder = new StringBuilder();

    public StringBuilder append(String str) {
        return builder.append(str);
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private boolean ended = false;

    public void passwordComplete() {
        if (!ended) {
            ended = true;
            onPasswordComplete(toString());
        }
    }

    protected void onPasswordComplete(String s) {
    }
}
