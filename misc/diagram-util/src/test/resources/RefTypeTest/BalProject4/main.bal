enum DepartmentCode {
    HR,
    IT,
    FINANCE
}

type Department record {
    string name;
    DepartmentCode code;
};

type Employee record {
    string name;
    Department department;
};


function fn1() {

    Department engineering = {
        name: "Engineering",
        code: IT
    };

    Employee alice = {
        name: "Alice",
        department: engineering
    };
}

type MyType2 record {|
    record {|
        int p;
        record {|
            int r;
        |} q;
    |}[] arr;
    record {|
        record {|
            int Ccc;
        |} Bb;
    |} Aa;
|};
