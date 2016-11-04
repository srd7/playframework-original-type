package exceptions

abstract class MyException(mes: String) extends RuntimeException(mes)

// 例外というより、エラーの型として取り扱う。
class UserNotFoundError extends MyException("user.not.found")
class PasswordWrongError extends MyException("password.is.wrong")
