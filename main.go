package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/", pgtCallback)
	http.ListenAndServe(":8080", nil)
}

func pgtCallback(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	fmt.Println(r.URL)
	w.WriteHeader(200)
}
