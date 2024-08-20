use std::{error::Error, mem::size_of, ptr};

use gl::types::{GLfloat, GLint, GLuint};

///
/// OpenGL VAO
///
pub struct VertexArrayObject {
    pub id: GLuint,
    pub vbo: GLuint, // vertex buffer object
    pub ebo: GLuint, // element buffer object
}

impl VertexArrayObject {

    ///
    /// Create a new VAO from vertices and indices
    ///
    /// # Arguments
    ///
    /// * `vertices` - Vertices of the VAO (x, y, z; u, v)
    /// * `indices` - Indices of the VAO (triangles)
    ///
    pub fn new(vertices: &[GLfloat], indices: &[GLuint]) -> Result<Self, Box<dyn Error>> {
        // create buffers
        let id = unsafe { VertexArrayObject::create_bound_vao() };
        let vbo = unsafe { VertexArrayObject::create_bound_vbo(vertices)? };
        let ebo = unsafe { VertexArrayObject::create_bound_ebo(indices)? };

        // set vertex attributes
        unsafe {
            gl::VertexAttribPointer(0, 3, gl::FLOAT, gl::FALSE, (5 * size_of::<GLfloat>()) as GLint, ptr::null());
            gl::EnableVertexAttribArray(0);
            gl::VertexAttribPointer(1, 2, gl::FLOAT, gl::FALSE, (5 * size_of::<GLfloat>()) as GLint, (3 * size_of::<GLfloat>()) as *const _);
            gl::EnableVertexAttribArray(1);
        }

        // unbind buffers
        unsafe {
            gl::BindBuffer(gl::ARRAY_BUFFER, 0);
            gl::BindBuffer(gl::ELEMENT_ARRAY_BUFFER, 0);
            gl::BindVertexArray(0);
        }

        Ok(VertexArrayObject { id, vbo, ebo })
    }

    unsafe fn create_bound_vao() -> GLuint {
        let mut id = 0;
        gl::GenVertexArrays(1, &mut id);
        gl::BindVertexArray(id);
        id
    }

    unsafe fn create_bound_vbo(data: &[GLfloat]) -> Result<GLuint, Box<dyn Error>> {
        let mut id = 0;
        gl::GenBuffers(1, &mut id);
        gl::BindBuffer(gl::ARRAY_BUFFER, id);
        gl::BufferData(gl::ARRAY_BUFFER, (data.len() * size_of::<GLfloat>()) as isize, data.as_ptr() as *const _, gl::STATIC_DRAW);
        if gl::GetError() != gl::NO_ERROR {
            return Err("failed to buffer data into the vertex buffer object".into());
        }
        Ok(id)
    }

    unsafe fn create_bound_ebo(data: &[GLuint]) -> Result<GLuint, Box<dyn Error>> {
        let mut id = 0;
        gl::GenBuffers(1, &mut id);
        gl::BindBuffer(gl::ELEMENT_ARRAY_BUFFER, id);
        gl::BufferData(gl::ELEMENT_ARRAY_BUFFER, (data.len() * size_of::<GLuint>()) as isize, data.as_ptr() as *const _, gl::STATIC_DRAW);
        if gl::GetError() != gl::NO_ERROR {
            return Err("failed to buffer data into the element buffer object".into());
        }
        Ok(id)
    }

    ///
    /// Bind the VAO
    ///
    pub fn bind(&self) {
        unsafe {
            gl::BindVertexArray(self.id);
            gl::BindBuffer(gl::ARRAY_BUFFER, self.vbo);
            gl::BindBuffer(gl::ELEMENT_ARRAY_BUFFER, self.ebo);
        }
    }

    ///
    /// Unbind the VAO
    ///
    pub fn unbind(&self) {
        unsafe {
            gl::BindVertexArray(0);
            gl::BindBuffer(gl::ARRAY_BUFFER, 0);
            gl::BindBuffer(gl::ELEMENT_ARRAY_BUFFER, 0);
        }
    }

}

impl Drop for VertexArrayObject {
    fn drop(&mut self) {
        unsafe {
            gl::DeleteVertexArrays(1, &self.id);
            gl::DeleteBuffers(1, &self.vbo);
            gl::DeleteBuffers(1, &self.ebo);
        }
    }
}