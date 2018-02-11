import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {

            new Main().start();
    }


    private enum  Operation {
        START_BRACKET(40),
        END_BRACKET(40),
        EXPONENTIATION(30),
        MULTIPLICATION(20),
        DIVISION(20),
        ADDITION(10),
        SUBTRACTION(10),
        EXECUTE(5);

        private int priority;

        Operation(int priority){
            this.priority = priority;
        }

        public int getPriority(){
            return priority;
        }
    }

    private Stack<Operation> operations = new Stack<>();
    private Stack<BigDecimal> operands = new Stack<>();
    private Scanner in =  new Scanner(System.in);
    private PrintStream out = new PrintStream(System.out);
    private PrintStream err = new PrintStream(System.err);

    private boolean nextUnary = true;
    private int countBracket = 0;
    private boolean repeat = false;
    private boolean lastOp = false;
    private boolean previousOperand = false;

    private void start(){

        while (true) {
            if (in.hasNextLine()) {
                String source = in.nextLine();
                if (source.equals("")) {
                    break;
                }
                init();
                try{
                    parsAndExecute(source);
                }catch (Exception e){
                    err.println(e.getMessage());
                    continue;
                }
                out.println(operands.peek());
            }
        }
        in.close();
        out.close();
        err.close();
    }

    private void init(){
        nextUnary = true;
        countBracket = 0;
        repeat = false;
        lastOp = false;
        previousOperand = false;

        operations.clear();
        operands.clear();
    }

    private void parsAndExecute(String source) throws Exception {

        Pattern operationPattern = Pattern.compile("[()^*/+-]");
        Pattern numeralPattern = Pattern.compile("[0-9.,]");

        for (int i = 0; i < source.length(); i++){
            String item = String.valueOf(source.charAt(i));
            if (item.equals(" ")) {
                continue;
            } else if (operationPattern.matcher(item).matches()) {
                parsOperation(item, i);
            }else if (numeralPattern.matcher(item).matches()){
                StringBuilder argument = new StringBuilder();
                boolean error = false;
                boolean comma = false;
                char newChar = source.charAt(i);

                while (newChar == '0' || newChar == '1' || newChar == '2'
                        || newChar == '3' || newChar == '4' || newChar == '5'
                        || newChar == '6' || newChar == '7' || newChar == '8'
                        || newChar == '9' || newChar == '.' || newChar == ','){

                    if (newChar == ',' || newChar == '.'){
                        if(comma){
                            error = true;
                        }
                        comma = true;
                        if (newChar == ','){
                            newChar = '.';
                        }
                    }
                    argument.append(newChar);
                    i++;
                    if (i < source.length()){
                        newChar = source.charAt(i);
                    }else {
                        break;
                    }
                }
                i--;
                if (previousOperand){
                    throw new Exception("repeat operand_: "
                                        + argument.toString());
                }
                previousOperand = true;
                if(error){
                    throw new Exception("invalid operand_: "
                                        + argument.toString());
                }
                BigDecimal operand = new BigDecimal(argument.toString());
                nextUnary = false;
                repeat = false;
                lastOp = false;

                operands.push(operand);
            }else {
                throw new Exception("impermissible char, position: "
                                    + (i + 1));
            }
        }
        if (countBracket != 0){
            throw new Exception("invalid operation: brackets");
        }
        if (repeat) {
            throw new Exception("invalid operation");
        }
        doOperand(Operation.EXECUTE);
    }

    private void parsOperation(String operation, int position) throws Exception{
        previousOperand = false;
        switch (operation) {
            case "(":
                pushOperator(Operation.START_BRACKET);
                nextUnary = true;
                lastOp = false;
                countBracket++;
                break;
            case ")":
                countBracket--;
                if (countBracket < 0){
                    throw new Exception("invalid operation:"
                            + " brackets, position: "
                            + (position + 1));
                }
                if (lastOp) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }
                nextUnary = false;
                pushOperator(Operation.END_BRACKET);
                break;
            case "^":
                if (nextUnary || repeat) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }
                repeat = true;
                lastOp = true;
                pushOperator(Operation.EXPONENTIATION);
                nextUnary = true;
                break;
            case "*":
                if (nextUnary || repeat) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }
                repeat = true;
                lastOp = true;
                pushOperator(Operation.MULTIPLICATION);
                nextUnary = false;
                break;
            case "/":
                if (nextUnary || repeat) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }
                repeat = true;
                lastOp = true;
                pushOperator(Operation.DIVISION);
                nextUnary = false;
                break;
            case "+":
                if (nextUnary) {
                    break;
                }
                if (repeat) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }
                repeat = true;
                lastOp = true;
                nextUnary = false;
                pushOperator(Operation.ADDITION);
                break;
            case "-":
                if (nextUnary) {
                    operands.push(new BigDecimal("-1"));
                    operations.push(Operation.MULTIPLICATION);
                }else  if (repeat) {
                    throw new Exception("invalid operation,"
                            + " position: " + (position + 1));
                }else {
                    repeat = true;
                    lastOp = true;
                    pushOperator(Operation.SUBTRACTION);
                }
                break;
        }
    }


    private void pushOperator(Operation operator) throws Exception {

        if (operator == Operation.END_BRACKET){
            doOperand(operator);
        }else if (operations.empty()
                  || operator == Operation.START_BRACKET
                  || operations.peek() == Operation.START_BRACKET){
            operations.push(operator);
        } else {
            Operation lastOperation  = operations.peek();
            if (operator.getPriority() <= lastOperation.getPriority()){
                doOperand(operator);
            }else {
                operations.push(operator);
            }
        }
    }

    private void doOperand(Operation sourceOperation) throws Exception {

        while (true){
            if (operations.empty()){
                return;
            }
            Operation currentOperation = operations.peek();

            if (currentOperation == Operation.START_BRACKET
                        && !(sourceOperation == Operation.END_BRACKET)){
                break;
            }
            operations.pop();

            if (currentOperation == Operation.START_BRACKET){
                return;
            }
            BigDecimal temp = operands.pop();
            BigDecimal result = operands.pop();
            switch (currentOperation){
                case EXPONENTIATION:
                    int num = temp.intValue();
                    if (num < 0 ){
                        num = num * (-1);
                        BigDecimal unit = new BigDecimal("1");
                        result = unit.divide(result.pow(num),
                                             2,
                                             RoundingMode.HALF_UP);
                    }else {
                        result = result.pow(num);
                    }
                    break;
                case MULTIPLICATION: result = result.multiply(temp);
                                     break;
                case       DIVISION: if (temp.intValue() == 0){
                                         throw new Exception("error: division "
                                                             + "by zero");
                                     }
                                     result = result.divide(temp,
                                                         2,
                                                         RoundingMode.HALF_UP);
                                     break;
                case       ADDITION: result = result.add(temp);
                                     break;
                case    SUBTRACTION: result = result.subtract(temp);
                                     break;
            }

            operands.push(result);

            if (operations.empty()){
                break;
            }
        }
        if (sourceOperation.ordinal() != 1 && sourceOperation.ordinal() != 7){
            operations.push(sourceOperation);
        }
    }
}



